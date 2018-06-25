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

public class StaticProviderMacrotrends extends ABaseStaticProvider {

  /**
   * format of request:
   * 
   * http://download.macrotrends.net/assets/php/stock_data_export.php?t=msft
   */

  @Override
  public ArrayList<Bar> getData(String symbol, LocalDateTime ldt1, LocalDateTime ldt2,
      LocalTime lt1, LocalTime lt2, int timeframe) {

    ArrayList<Bar> dataItems = new ArrayList<Bar>();
    String strUrl = "http://download.macrotrends.net/assets/php/stock_data_export.php?t=" + symbol;
    try {
      URL url = new URL(strUrl);
      try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String inputLine;
        // skip description
        while ((inputLine = in.readLine()) != null) {
          if (inputLine.startsWith("date"))
            break;
        }
        in.readLine(); // skip header line
        while ((inputLine = in.readLine()) != null) {
          StringTokenizer st = new StringTokenizer(inputLine, ",");
          Date date = df.parse(st.nextToken());
          if (date.getTime() > java.sql.Timestamp.valueOf(ldt2).getTime())
            break;
          if (date.getTime() < java.sql.Timestamp.valueOf(ldt1).getTime())
            continue;
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

  @Override
  public String getDescription() {
    return "Macrotrends data source (DAILY)";
  }

  @Override
  public void load() {
  }

  @Override
  public void unload() {
  }

}
