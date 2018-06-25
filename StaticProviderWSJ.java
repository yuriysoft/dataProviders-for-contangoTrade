package contangoStaticProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import contangoAPI.api.ABaseStaticProvider;
import contangoAPI.api.Bar;

public class StaticProviderWSJ extends ABaseStaticProvider {

  /**
   * format of request:
   * 
   * quotes.wsj.com/msft/historical-prices/download?MOD_VIEW=page
   *     &num_rows=6299.041666666667&range_days=6299.041666666667
   *     &startDate=09/06/2000&endDate=12/05/2017
   */

  @Override
  public ArrayList<Bar> getData(String symbol, LocalDateTime ldt1, LocalDateTime ldt2,
      LocalTime lt1, LocalTime lt2, int timeframe) {

    ArrayList<Bar> dataItems = new ArrayList<Bar>();
    String strUrl = getURL(symbol, ldt1, ldt2);
    try {
      URL url = new URL(strUrl);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
        final DateFormat df = new SimpleDateFormat("MM/dd/yy", Locale.ENGLISH);
        String inputLine;
        in.readLine(); // skip 1-st line
        while ((inputLine = in.readLine()) != null) {
          StringTokenizer st = new StringTokenizer(inputLine, ",");
          Date date = df.parse(st.nextToken());
          if (date.getTime() > java.sql.Timestamp.valueOf(ldt2).getTime())
            break;
          double open = Double.parseDouble(st.nextToken());
          double high = Double.parseDouble(st.nextToken());
          double low = Double.parseDouble(st.nextToken());
          double close = Double.parseDouble(st.nextToken());
          double volume = Double.parseDouble(st.nextToken());
          dataItems.add(new Bar(open, close, high, low, volume, date));
        }
      }
    } catch (ParseException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(String.format("URL problem: %s", strUrl), e);
    }
    return dataItems;
  }

  /**
   * Get date as string
   * 
   * @param ldt:
   *          local date time
   * @return date: as string
   */
  private static String dt2String(LocalDateTime ldt) {
    return String.format("%02d/%02d/%04d", ldt.getMonthValue(), ldt.getDayOfMonth(), ldt.getYear());
  }

  /**
   * Prepare URL string for the data provider
   * 
   * @param symbol:
   *          security code
   * @param ldt1:
   *          start date
   * @param ldt2:
   *          end date
   * @return URL string
   */
  private static String getURL(String symbol, LocalDateTime ldt1, LocalDateTime ldt2) {
    StringBuilder buf = new StringBuilder();
    buf.append("https://quotes.wsj.com/").append(symbol);
    buf.append("/historical-prices/download?");
    buf.append("MOD_VIEW=page&num_rows=6299.041666666667&range_days=6299.041666666667");
    buf.append("&startDate=").append(dt2String(ldt1));
    buf.append("&endDate=").append(dt2String(ldt2));
    return buf.toString();
  }

  @Override
  public String getDescription() {
    return "Wall Street Journal data source (DAILY)";
  }

  @Override
  public void load() {
  }

  @Override
  public void unload() {
  }

}
