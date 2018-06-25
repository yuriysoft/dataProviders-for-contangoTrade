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

public class StaticProviderQuotemedia extends ABaseStaticProvider {
  
  /** 
   * format of request:
   * 
   * http://app.quotemedia.com/quotetools/getHistoryDownload.csv?
   *   &webmasterId=501&startDay=1&startMonth=1&startYear=1900&endDay=17&endMonth=5
   *   &endYear=2017&isRanged=true&symbol=[SYMBOL NAME]
   */

  @Override
  public ArrayList<Bar> getData(String symbol, LocalDateTime ldt1, LocalDateTime ldt2, LocalTime lt1, LocalTime lt2,
      int timeframe) {

    ArrayList<Bar> dataItems = new ArrayList<Bar>();
    String strUrl = getURL(symbol, ldt1, ldt2);
    try {
      URL url = new URL(strUrl);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
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
   * Prepare URL string for the data provider
   * @param symbol: security code
   * @param ldt1: start date
   * @param ldt2: end date
   * @return URL string
   */
  private static String getURL(String symbol, LocalDateTime ldt1, LocalDateTime ldt2) {
    StringBuilder buf = new StringBuilder();
    buf.append("http://app.quotemedia.com/quotetools/getHistoryDownload.csv?&webmasterId=501");
    buf.append("&startDay=").append(String.valueOf(ldt1.getDayOfMonth()));
    buf.append("&startMonth=").append(String.valueOf(ldt1.getMonthValue() - 1));
    buf.append("&startYear=").append(String.valueOf(ldt1.getYear()));
    buf.append("&endDay=").append(String.valueOf(ldt2.getDayOfMonth()));
    buf.append("&endMonth=").append(String.valueOf(ldt2.getMonthValue() - 1));
    buf.append("&endYear=").append(String.valueOf(ldt2.getYear()));
    buf.append("&isRanged=true&symbol=").append(symbol);
    return buf.toString();
  }
  
  @Override
  public String getDescription() {
    return "Quotemedia data source (DAILY)";
  }

  @Override
  public void load() {
  }

  @Override
  public void unload() {
  }

}
