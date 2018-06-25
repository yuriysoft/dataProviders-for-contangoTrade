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

import contangoAPI.api.ABaseStaticProvider;
import contangoAPI.api.Bar;

public class StaticProviderFinContent extends ABaseStaticProvider {

  /**
   * format of request:
   * 
   * http://markets.financialcontent.com/stocks/action/gethistoricaldata?Month=12
   * &Symbol=[SYMBOL NAME]&Range=300&Year=2017
   */

  @Override
  public ArrayList<Bar> getData(String symbol, LocalDateTime ldt1, LocalDateTime ldt2,
      LocalTime lt1, LocalTime lt2, int timeframe) {

    ArrayList<Bar> dataItems = new ArrayList<Bar>();
    String strUrl = getURL(symbol, ldt2);
    try {
      URL url = new URL(strUrl);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
        final DateFormat df = new SimpleDateFormat("MM/dd/yy", Locale.ENGLISH);
        String inputLine;
        in.readLine(); // skip header line
        myLabel: while ((inputLine = in.readLine()) != null) {
          String[] ar = inputLine.split(",");
          // check and skip damaged values
          for(int i = 1; i < 7; i++) {
            if (ar[i].equals("")) {
              continue myLabel;
            }
          }
          Date date = df.parse(ar[1]);
          if (date.getTime() < java.sql.Timestamp.valueOf(ldt1).getTime())
            continue;
          dataItems.add(new Bar(
              Double.parseDouble(ar[2]),
              Double.parseDouble(ar[5]),
              Double.parseDouble(ar[3]),
              Double.parseDouble(ar[4]),
              Double.parseDouble(ar[6]),
              date
              ));
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
   * 
   * @param symbol:
   *          security code
   * @param ldt1:
   *          start date
   * @param ldt2:
   *          end date
   * @return URL string
   */
  private static String getURL(String symbol, LocalDateTime ldt2) {
    StringBuilder buf = new StringBuilder();
    buf.append("http://markets.financialcontent.com/stocks/action/gethistoricaldata?Symbol=")
        .append(symbol);
    buf.append("&Range=300");
    buf.append("&Month=").append(ldt2.getMonthValue());
    buf.append("&Year=").append(ldt2.getYear());
    return buf.toString();
  }

  @Override
  public String getDescription() {
    return "Financial Content data source (DAILY)";
  }

  @Override
  public void load() {
  }

  @Override
  public void unload() {
  }

}
