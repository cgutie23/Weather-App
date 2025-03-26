import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/*Retrieve weather data from API - This backend will fetch the latest
  data from the external API and return it. The GUI will display this data to the user.*/
public class WeatherApp {
    //fetch weather data for given location
    public static JSONObject getWeatherData(String locationName){
        //Get location coordinates using the geolocation API
        JSONArray locationData = getLocationData(locationName);

        //extract lat and long data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
         "latitude=" + latitude + "&longitude=" + longitude +
            "&hourly=temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m";

        try{
            //call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check for response status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            //Store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());

            while(scanner.hasNext()){
                //read and store into the string builder
                resultJson.append(scanner.nextLine());
            }

            //close scanner and url connection
            scanner.close();
            conn.disconnect();

            //Parse through our data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            //retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            //want to get current hours data, so we need index of current hour
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            //get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            //get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            //get humidity
            JSONArray realtiveHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) realtiveHumidity.get(index);

            //get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            //build the weather json data object that we are going to access in our frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    //retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName){
        //replace whitespace in location name to + to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        //build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            //call API and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //Check response status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }
            else{
                //Store the API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                //Read and store the resulting json data into our string builder
                while(scanner.hasNext()){
                    resultJson.append(scanner.nextLine());
                }

                //Close scanner
                scanner.close();

                //Close url connection
                conn.disconnect();

                //parse the JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                //get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        //Couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            //Attempt to create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            //set request method to get
            conn.setRequestMethod("GET");

            //connect to our API
            conn.connect();
            return conn;
        }catch(IOException e){
            e.printStackTrace();
        }

        //could not make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        //Iterate through the time list and see which one matches our current time
        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                //return the index
                return i;
            }
        }

        return 0;
    }

    public static String getCurrentTime(){
        //get current data and time
        LocalDateTime currentDateTime = LocalDateTime.now();

        //format date to be YYYY-MM-DDT00:00 (this is how it is read in the API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        //format and print the current date and time
        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    //Convert the weather code to something more readable
    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";

        if(weathercode == 0L){
            //clear
            weatherCondition = "Clear";
        }
        else if(weathercode > 0L && weathercode <= 3L){
            //cloudy
            weatherCondition = "Cloudy";
        }
        else if((weathercode >= 51L && weathercode <= 67L)
                    || (weathercode >= 80L && weathercode <= 99L)){
            //rainy
            weatherCondition = "Rain";
        }
        else if(weathercode >= 71L && weathercode <= 77L){
            //snow
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}
