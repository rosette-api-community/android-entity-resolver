/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.basistech.EntityExtractionApp;

import android.app.assist.AssistContent;
import android.app.assist.AssistStructure;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.service.voice.VoiceInteractionSession;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.basistech.rosette.apimodel.EntitiesOptions;
import com.basistech.rosette.apimodel.LinkedEntitiesResponse;
import com.basistech.rosette.apimodel.LinkedEntity;
import com.basistech.rosette.apimodel.SentimentResponse;
import com.basistech.util.LanguageCode;
import com.google.common.base.Function;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutionException;


public class ExtractionInteractionSession extends VoiceInteractionSession {

    Requests.LinkedEntitiesRequest task;
    Requests.EntityLevelSentimentRequest sentiTask;
    HashMap<String, List<String>> entities;
    HashMap<Button, LinkedEntity> buttonEntityMap;
    List<LinkedEntity> listOfEntity;
    TableLayout tableForEntities;
    Requests.LanguageIdentificationRequest idRequest;
    LanguageCode identifiedLanguage = null;
    Requests requests = null;

    public ExtractionInteractionSession(Context context) {
        super(context);
    }

    public ExtractionInteractionSession(Context context, Handler handler) {
        super(context, handler);
    }

    @Override
    public void onHandleAssist(Bundle data, AssistStructure structure, AssistContent content) {
        super.onHandleAssist(data, structure, content);
        StringBuilder text = new StringBuilder();
        if (structure == null) {
            return;
        }
        if (content.getClipData() != null) {
            text.append(content.getClipData().toString());
        }
        //text.append("Donald Trump wanted to make sure there were some entities to extract");
        text.append(extractText(structure));
        String finalText = text.toString();
        String apiKey = new SettingsActivity.MyPreferenceFragment().getPreferenceManager()
                .getSharedPreferences().getString("prefRosetteAPIKey", "NULL");
        String altUrl = new SettingsActivity.MyPreferenceFragment().getPreferenceManager()
                .getSharedPreferences().getString("prefRosetteAltURL", "NULL");
        requests = new Requests(apiKey, altUrl);
        task = (Requests.LinkedEntitiesRequest) requests.new LinkedEntitiesRequest(new
                EntitiesOptions()).execute(new Pair<>(finalText, LanguageCode.UNKNOWN));
        sentiTask = (Requests.EntityLevelSentimentRequest)
                requests.new EntityLevelSentimentRequest(LanguageCode.ENGLISH).execute(finalText);
        idRequest = (Requests.LanguageIdentificationRequest) requests.new
                LanguageIdentificationRequest().execute(finalText);
        ExtractionActivity.clearEntities(tableForEntities);
        buttonEntityMap = new HashMap<>();
        populate();

        if (idRequest != null) {
            try {
                identifiedLanguage = idRequest.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                return;
            }
        } else {
            identifiedLanguage = LanguageCode.UNKNOWN;
        }

        if (!Requests.localeLanguage.equals(identifiedLanguage)) {
            ExtractionActivity.offerTranslation(tableForEntities, getContext(), new Runnable() {
                @Override
                public void run() {
                    doNameTranslation();
                }
            });
        }

    }

    private void doNameTranslation() {
        for (int i = 0; i < tableForEntities.getChildCount(); i++) {
            Button button = (Button) ((TableRow) tableForEntities.getChildAt(i)).getChildAt(0);
            LinkedEntity relevantEntity = buttonEntityMap.get(button);
            if (relevantEntity != null) { // && relevantEntity.ExtractionActivity
            // .translateButtonToEnglish(button, id, false);
                requests.new TranslateButtonToLocaleRequest(identifiedLanguage, button).execute();
            }
        }

    }


    private String extractText(AssistStructure structure) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < structure.getWindowNodeCount(); i++) {
            AssistStructure.WindowNode windowNode = structure.getWindowNodeAt(i);
            text.append(structure.getWindowNodeAt(i).getRootViewNode().getText());
            Stack<AssistStructure.ViewNode> toCheck = new Stack<>();
            toCheck.add(windowNode.getRootViewNode());

            while (!toCheck.empty()) {
                AssistStructure.ViewNode checking = toCheck.pop();
                CharSequence checkingText = checking.getText();
                if (checkingText != null) {
                    text.append(wrapInSpaces(checkingText));
                }
                for (int j = 0; j < checking.getChildCount(); j++) {
                    toCheck.add(checking.getChildAt(j));
                }
            }
        }
        return text.toString();
    }

    private String wrapInSpaces(CharSequence s) {
        return " " + s + " ";
    }

    @Override
    public void onRequestConfirmation(ConfirmationRequest request) {
    }

    @Override
    public void onRequestPickOption(PickOptionRequest request) {
    }

    @Override
    public void onRequestCommand(CommandRequest request) {
    }

    @Override
    public void onCancelRequest(Request request) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public View onCreateContentView() {
        View v = getLayoutInflater().inflate(R.layout.assist, null);
        tableForEntities = (TableLayout) v.findViewById(R.id.tableForEntities);
        return v;
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
    }

    private void populate() {
        LinkedEntitiesResponse response;
        SentimentResponse sentiResponse;
        try {
            response = task.get();
            sentiResponse = sentiTask.get();
            if (response == null) {
                return;
            }
            entities = ExtractionActivity.retrieveLinkedEntities(response.getEntities(),
                    sentiResponse.getEntities());
            listOfEntity = response.getEntities();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ExtractionActivity.populateEntities(listOfEntity, entities, tableForEntities, getContext
                (), buttonEntityMap, new Function<String, Void>() {
            @Override
            public Void apply(String input) {
                gridButtonClicked(input);
                return null;
            }
        });
    }

    private void gridButtonClicked(String wiki) {
        Intent browserIntent =
                new Intent(Intent.ACTION_VIEW, Uri.parse(wiki));
        browserIntent.setFlags(browserIntent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
        //required by Android
        getContext().startActivity(browserIntent);
    }

    @Override
    public void onHide() {
        super.onHide();
    }
}
