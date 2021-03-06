package applications.ShowAPIClient.APIs;

import com.google.gson.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;
import shared.show.*;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Created by Maiko on 17-6-2017.
 */
public class EpisoDateAPIReader implements IAPIReader {

    public final SimpleDateFormat sdfShow = new SimpleDateFormat("yyyy-MM-dd");
    public final SimpleDateFormat sdfEpisode = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public ArrayList<Show> getShow(String request) {
        return new ArrayList<Show>() {{ add(jsonToShow(getRequest("https://www.episodate.com/api/show-details?q=" + request))); }};
    }

    public ArrayList<Show> searchShow(String request) {
        int page = 1;
        String json = getRequest("https://www.episodate.com/api/search?q=" + request + "&page=" + page);
        int pages = Integer.parseInt(new JsonParser().parse(json).getAsJsonObject().get("pages").toString());
        ArrayList<Show> shows = jsonToShows(json);
        while (pages > page) {
            page++;
            shows.addAll(jsonToShows(getRequest("https://www.episodate.com/api/search?q=" + request + "&page=" + page)));
        }
        return shows;
    }

    /**
     * Sends a get request to the requested url
     * @param request requested url
     * @return returns the server reply (Json)
     */
    private String getRequest(String request) {
        System.out.println(request);
        String result = new String();

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet getRequest = new HttpGet(request);
            //getRequest.addHeader("accept", "application/json");
            HttpResponse response = httpClient.execute(getRequest);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(((response.getEntity().getContent()))));
            String output;
            System.out.println("Output from Server .... \n");
            while ((output = reader.readLine()) != null) {
                System.out.println(output);
                result += output;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Translates the json string to tvShow object
     */
    private Show jsonToShow(String json) {
        Show show = new Show();
        try {
            //JsonObject with all show properties
            JsonObject jShow = new JsonParser().parse(json).getAsJsonObject().getAsJsonObject("tvShow");

            show.setSource("EpisoDate");
            show.setSourceId(Integer.parseInt(jShow.get("id").toString()));
            show.setRequestDate(Calendar.getInstance());
            show.setStatus(stringToStatus(jShow.get("status").toString()));
            show.setCountdown(jsonToEpisode(jShow.get("countdown").getAsJsonObject()));

            ArrayList<Episode> episodes = new ArrayList<>();
            Iterator<JsonElement> episodeIterator = jShow.get("episodes").getAsJsonArray().iterator();
            while (episodeIterator.hasNext()) {
                Episode episode = jsonToEpisode(episodeIterator.next().getAsJsonObject());
                if (episode != null)
                    episodes.add(episode);
            }

            show.setEpisodes(episodes);
            show.setName(jShow.get("name").toString().replace("\"", ""));
            show.setSource(jShow.get("url").toString().replace("\"", ""));
            show.setDescription(jShow.get("description").toString().replace("\"", ""));
            show.setDescriptionSource(jShow.get("description_source").toString().replace("\"", ""));

            //Parse start and endDate to calender objects
            try {
                Calendar tempCal = Calendar.getInstance();
                tempCal.setTime(sdfShow.parse(jShow.get("start_date").toString().replace("\"", "")));
                show.setStartDate(tempCal);

                //End date can be null if the show hasn't ended yet
                String endDate = jShow.get("end_date").toString().replace("\"", "");
                if (endDate.equals("null")) {
                    show.setEndDate(null);
                }
                else {
                    tempCal.setTime(sdfShow.parse(endDate));
                    show.setEndDate(tempCal);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            show.setCountry(jShow.get("country").toString().replace("\"", ""));
            show.setRunTime(Integer.parseInt(jShow.get("runtime").toString()));
            show.setNetwork(jShow.get("network").toString().replace("\"", ""));
            show.setYoutubeLink(jShow.get("youtube_link").toString().replace("\"", ""));
            show.setImagePath(jShow.get("image_path").toString().replace("\"", ""));
            show.setImageThumbnailPath(jShow.get("image_thumbnail_path").toString().replace("\"", ""));
            show.setRating(Double.parseDouble(jShow.get("rating").toString().replace("\"", "")));
            show.setRatingCount(Integer.parseInt(jShow.get("rating_count").toString().replace("\"", "")));
            Gson gson = new Gson();
            show.setGenres(gson.fromJson(jShow.get("genres").toString(), String[].class));
            show.setPictures(gson.fromJson(jShow.get("pictures").toString(), String[].class));
        } catch (JsonParseException e) {
            e.printStackTrace();
        }
        return show;
    }

    private Episode jsonToEpisode(JsonObject jEpisode) {
        Episode episode = new Episode();
        episode.setSeasonNr(Integer.parseInt(jEpisode.get("season").toString()));
        episode.setEpisodeNr(Integer.parseInt(jEpisode.get("episode").toString()));
        episode.setName(jEpisode.get("name").toString().replace("\"", ""));
        try {
            Calendar tempCal = Calendar.getInstance();
            tempCal.setTime(sdfEpisode.parse(jEpisode.get("air_date").toString().replace("\"", "")));
            episode.setAirDate(tempCal);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return episode;
    }

    /**
     * Translates the json string to tvShow objects
     * Used when the user queses a show
     */
    private ArrayList<Show> jsonToShows(String json) {
        ArrayList<Show> shows = new ArrayList<>();
        try {
            //JsonElement with all shows
            Iterator<JsonElement> showIterator = new JsonParser().parse(json).getAsJsonObject().get("tv_shows").getAsJsonArray().iterator();
            while (showIterator.hasNext()) {
                JsonObject jShow = showIterator.next().getAsJsonObject();
                Show show = new Show();
                show.setSource("EpisoDate");
                show.setSourceId(Integer.parseInt(jShow.get("id").toString()));
                show.setName(jShow.get("name").toString().replace("\"", ""));
                show.setCountry(jShow.get("country").toString().replace("\"", ""));
                show.setNetwork(jShow.get("network").toString().replace("\"", ""));
                show.setStatus(stringToStatus(jShow.get("status").toString()));
                show.setImageThumbnailPath(jShow.get("image_thumbnail_path").toString().replace("\"", ""));
                shows.add(show);
            }
        } catch (JsonParseException e) {
            e.printStackTrace();
        }
        return shows;
    }

    private Status stringToStatus(String status) {
        return Status.valueOf(status.replace("\"", "").replace("/", "_").replace(" ", "_").toUpperCase());
    }
}
