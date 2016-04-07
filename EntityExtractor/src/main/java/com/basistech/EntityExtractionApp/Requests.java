package com.basistech.EntityExtractionApp;

import android.os.AsyncTask;
import android.util.Pair;
import android.widget.Button;

import com.basistech.rosette.api.RosetteAPI;
import com.basistech.rosette.api.RosetteAPIException;
import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.EntitiesResponse;
import com.basistech.rosette.apimodel.LanguageOptions;
import com.basistech.rosette.apimodel.LinkedEntitiesResponse;
import com.basistech.rosette.apimodel.NameTranslationRequest;
import com.basistech.rosette.apimodel.NameTranslationResponse;
import com.basistech.rosette.apimodel.SentimentOptions;
import com.basistech.rosette.apimodel.SentimentResponse;
import com.basistech.util.LanguageCode;
import com.google.common.collect.ImmutableSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;

/**
 * Created by danderson on 2/26/16.
 */
public class Requests {

    public static LanguageCode localeLanguage = LanguageCode.lookupByISO639(Locale.getDefault()
            .getISO3Language());
    public static Set<LanguageCode> nameTranslationTargets = ImmutableSet.of(LanguageCode.ENGLISH);

    String apiKey;
    String altUrl;
    private RosetteAPI rosetteAPI;

    public Requests(String apiKey, String altUrl) {
        this.apiKey = apiKey;
        this.altUrl = altUrl;
        rosetteAPI = getRosetteApiHandle(this.apiKey, this.altUrl);
    }

    public class EntitiesRequest extends AsyncTask<Pair<String, LanguageCode>, Void,
            EntitiesResponse> {
        EntitiesOptions options;

        public EntitiesRequest(EntitiesOptions options) {
            super();
            this.options = options;
        }

        @SafeVarargs
        @Override
        protected final EntitiesResponse doInBackground(Pair<String, LanguageCode>... params) {
            try {
                return rosetteAPI.getEntities(params[0].first, params[0].second, options);
            } catch (RosetteAPIException | IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public RosetteAPI getRosetteApiHandle(String apiKey, String altUrl) {
        if (rosetteAPI == null) {
            try {
                AsyncRosetteRequest task = (AsyncRosetteRequest) new AsyncRosetteRequest()
                        .execute(apiKey, altUrl);
                rosetteAPI = task.get();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return rosetteAPI;
    }

    public class AsyncRosetteRequest extends AsyncTask<String, Void, RosetteAPI> {
        @Override
        protected RosetteAPI doInBackground(String... params) {
            if (params[0] == null || params[0].isEmpty()) {
                return null;
            }
            try {
                if (params[1] == null || params[1].isEmpty()) {
                    return new RosetteAPI(params[0]);
                } else {
                    return new RosetteAPI(params[0], params[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (RosetteAPIException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class LinkedEntitiesRequest extends AsyncTask<Pair<String, LanguageCode>, Void,
            LinkedEntitiesResponse> {
        EntitiesOptions options;

        public LinkedEntitiesRequest(EntitiesOptions options) {
            super();
            this.options = options;
        }

        @SafeVarargs
        @Override
        protected final LinkedEntitiesResponse doInBackground(Pair<String, LanguageCode>...
                                                                      params) {
            if (params[0].first == null || params[0].first.isEmpty()) {
                return null;
            }
            try {
                return rosetteAPI.getLinkedEntities(params[0].first, params[0].second);
            } catch (IOException | RosetteAPIException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
                //return null;
            }
        }
    }

    public class AsyncWikiRequest extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            InputStream is = null;
            try {
                is = new URL(params[0]).openStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is, Charset.forName
                        ("UTF-8")));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public class LanguageIdentificationRequest extends AsyncTask<String, Void, LanguageCode> {
        @Override
        protected LanguageCode doInBackground(String... params) {
            try {
                rosetteAPI.getLanguage(params[0], new LanguageOptions());
            } catch (Exception e) { // gotta catch 'em all!
                e.printStackTrace();
            }
            return null;
        }
    }

    public class TranslateNameToLocaleRequest extends AsyncTask<String, Void,
            NameTranslationResponse> {
        LanguageCode origin;

        public TranslateNameToLocaleRequest(LanguageCode origin) {
            this.origin = origin;
        }

        @Override
        protected NameTranslationResponse doInBackground(String... params) {
            try {
                LanguageCode target = localeLanguage;
                if (!nameTranslationTargets.contains(target)) {
                    target = LanguageCode.ENGLISH;
                }

                NameTranslationRequest request = new NameTranslationRequest.Builder(params[0],
                        target).sourceLanguageOfUse(origin).build();
                return rosetteAPI.getNameTranslation(request);
            } catch (Exception e) { // gotta catch 'em all!
                e.printStackTrace();
            }
            return null;
        }
    }

    public class TranslateButtonToLocaleRequest extends AsyncTask<Void, Void, Void> {
        LanguageCode origin;
        String text;
        NameTranslationResponse response;
        Button target;

        public TranslateButtonToLocaleRequest(LanguageCode origin, Button target) {
            this.origin = origin;
            this.target = target;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            text = target.getText().toString();
        }

        @Override
        protected Void doInBackground(Void... params) {
            response = new TranslateNameToLocaleRequest(origin).doInBackground(text);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (response != null) {
                target.setText(response.getTranslation());
            } else {
                System.err.println("Houston, we have a name translation failure");
            }
        }
    }

    public class EntityLevelSentimentRequest extends AsyncTask<String, Void, SentimentResponse> {
        LanguageCode origin;

        public EntityLevelSentimentRequest(LanguageCode origin) {
            super();
            this.origin = origin;
        }

        @SafeVarargs
        @Override
        protected final SentimentResponse doInBackground(String... params) {
            if (params[0] == null || params[0].isEmpty()) {
                return null;
            }
            LanguageCode target = localeLanguage;
            if (!nameTranslationTargets.contains(target)) {
                target = LanguageCode.ENGLISH;
            }
            try {
                return rosetteAPI.getSentiment(params[0], target, null);
            } catch (IOException | RosetteAPIException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
                //return null;
            }
        }
    }
}