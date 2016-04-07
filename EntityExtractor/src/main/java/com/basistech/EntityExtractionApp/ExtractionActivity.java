package com.basistech.EntityExtractionApp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.EntitySentiment;
import com.basistech.rosette.apimodel.LinkedEntitiesResponse;
import com.basistech.rosette.apimodel.LinkedEntity;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.apimodel.SentimentResponse;
import com.basistech.util.LanguageCode;
import com.google.common.base.Function;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 * Created by danderson on 2/25/16.
 */
public class ExtractionActivity extends Activity {
    EntitiesOptions options;
    static Requests requests = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.extraction);
        Intent intent = getIntent();
        options = new EntitiesOptions();
        final String input = intent.getStringExtra(EntryActivity.TEXT);
        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
        final String api = sharedPrefs.getString("prefRosetteAPIKey", "NULL");
        final String url = sharedPrefs.getString("prefRosetteAltURL", "NULL");
        LinkedEntitiesResponse entitiesResponse;
        SentimentResponse sentiResponse;
        HashMap<String, List<String>> entities;
        HashMap<Button, LinkedEntity> buttonMap = new HashMap<>();
        try {
            requests = new Requests(api, url);
            Requests.LinkedEntitiesRequest entitiesTask = (Requests.LinkedEntitiesRequest) requests
                    .new
                    LinkedEntitiesRequest(options).execute(new Pair<>(input, LanguageCode.ENGLISH));
            Requests.EntityLevelSentimentRequest sentiTask = (Requests.EntityLevelSentimentRequest)
                    requests.new EntityLevelSentimentRequest(LanguageCode.ENGLISH).execute(input);
            entitiesResponse = entitiesTask.get();
            sentiResponse = sentiTask.get();
            if (entitiesResponse == null || sentiResponse == null) {
                return;
            }
            entities = retrieveLinkedEntities(entitiesResponse.getEntities(), sentiResponse.getEntities());
            populateEntities(entitiesResponse.getEntities(), entities, (TableLayout) findViewById
                    (R.id.tableForEntities), this, buttonMap, new Function<String, Void>() {
                @Override
                public Void apply(String input) {
                    gridButtonClicked(input);
                    return null;
                }

                @Override
                public boolean equals(Object object) {
                    return false;
                }
            });
        } catch (Exception e) {
            // Feel the evil flow through you
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Error");
            alertDialogBuilder
                    .setIcon(R.drawable.stat_notify_error)
                    .setMessage(e.getMessage())
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }
    }

    public static HashMap<String, List<String>> retrieveLinkedEntities
            (List<LinkedEntity> entities, Collection<EntitySentiment> sentiResponseEntities)
            throws ExecutionException, InterruptedException {
        HashMap<String, List<String>> map = new HashMap<String, List<String>>();
        List<EntitySentiment> sentiResponse = new ArrayList<EntitySentiment>(sentiResponseEntities);

        for (LinkedEntity entity : entities) {
            String key = entity.getMention();
            String value = null;
            List<String> qidAndSenti = new ArrayList<String>();
            try {
                String wikiData = entity.getEntityId();
                if (!wikiData.equals("NEW-CLUSTER")) {
                    value = getWikipediaUrl(wikiData);
                    qidAndSenti.add(value);
                    int i = findEntitySentimentIndex(sentiResponse, entity.getMention());
                    if (i != -1) {
                        qidAndSenti.add(sentiResponse.get(i).getSentiment().getLabel());
                    } else {
                        qidAndSenti.add("not-found");
                    }
                    map.put(key, qidAndSenti);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return map;
    }

    public static int findEntitySentimentIndex(List<EntitySentiment> sentiResponse, String
            mention) {
        for (int i = 0; i < sentiResponse.size(); i++) {
            if (sentiResponse.get(i).getMention().equals(mention)) {
                return i;
            }
        }
        return -1;
    }

    public static String getWikipediaUrl(String qid) throws IOException, JSONException,
            ExecutionException, InterruptedException {
        String url = "https://www.wikidata.org/w/api.php?action=wbgetentities&ids=" + qid
                + "&languages=en&props=sitelinks&sitefilter=enwiki&format=json";
        Requests.AsyncWikiRequest task = (Requests.AsyncWikiRequest) requests.new
                AsyncWikiRequest().execute(url);

        String wikipediaURL = "";
        String jsonText = task.get();
        if (jsonText == null) {
            return null;
        }
        JSONObject json = new JSONObject(jsonText);
        try {
            JSONObject entities = (JSONObject) json.get("entities");
            JSONObject id = (JSONObject) entities.get(qid);
            JSONObject sitelinks = (JSONObject) id.get("sitelinks");
            JSONObject enwiki = (JSONObject) sitelinks.get("enwiki");
            wikipediaURL = "https://en.wikipedia.org/wiki/" + enwiki.get("title").toString()
                    .replace(" ", "_");
        } catch (Exception e) {
            wikipediaURL = "https://www.wikidata.org/" + qid;
        }
        return wikipediaURL;
    }

    public static void populateEntities(final List<LinkedEntity> entities, final HashMap<String,
            List<String>> entityMap, TableLayout table, Context context, Map<Button,
            LinkedEntity>
                                                buttonMap, final Function<String, Void> onClick) {
        SentimentOptions options = new SentimentOptions();
        for (final LinkedEntity entity : entities) {
            TableRow tableRow = new TableRow(context);
            tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams
                    .MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            table.addView(tableRow);

            Button button = new Button(context);
            button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT, 1.0f));
            button.setText(entity.getMention());
            button.setHeight(50);
            setSentimentColor(button, entityMap.get(entity.getMention()).get(1));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClick.apply(entityMap.get(entity.getMention()).get(0));
                }
            });
            tableRow.addView(button);
            buttonMap.put(button, entity);
        }
    }

    public static void setSentimentColor(Button button, String sentiment) {
        int red = Color.parseColor("#df2a2a");
        int green = Color.parseColor("#53e353");
        int gray = Color.parseColor("#5a5454");

        if (sentiment.equals("neg")) {
            button.getBackground().setColorFilter(new LightingColorFilter(Color.RED,
                    red));
        } else if (sentiment.equals("pos")) {
            button.getBackground().setColorFilter(new LightingColorFilter(Color.GREEN,
                    green));
        } else {
            button.getBackground().setColorFilter(new LightingColorFilter(Color.GRAY, gray));
        }
    }

    public static void offerTranslation(TableLayout table, Context context, final Runnable
            ifOfferAccepted) {
        TableRow tableRow = new TableRow(context);
        tableRow.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams
                .MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        table.addView(tableRow, 0);

        Button button = new Button(context);
        button.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1.0f));
        button.setText("Translate"); //obviously this would need to be localized
        button.setHeight(50);
        int color = Color.parseColor("#78ddff");
        button.getBackground().setColorFilter(new LightingColorFilter(0x00FF00, color));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ifOfferAccepted.run();
            }
        });
        tableRow.addView(button);
    }

    public static void clearEntities(TableLayout table) {
        table.removeAllViews();
    }

    private void gridButtonClicked(String wiki) {
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(wiki));
        startActivity(browserIntent);
    }

    public void translateButtonToEnglish(Button button, LanguageCode origin, boolean block) {
        Requests.TranslateButtonToLocaleRequest request = (Requests
                .TranslateButtonToLocaleRequest) requests.new TranslateButtonToLocaleRequest
                (origin, button).execute();
        if (block) {
            try {
                request.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }
}