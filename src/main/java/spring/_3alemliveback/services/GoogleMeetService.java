package spring._3alemliveback.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class GoogleMeetService {

    private static final String APPLICATION_NAME = "3alem Live Meet Integration";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private boolean devMode = false;
    @Value("classpath:credentials.json")
    private Resource credentialsFile;

    /**
     * Crée un lien Google Meet pour une formation
     * @param titre Titre de la formation
     * @param description Description de la formation
     * @param dateDebut Date de début de la formation
     * @param dateFin Date de fin de la formation
     * @return URL du Meet créé
     */
    public String createMeetLink(String titre, String description, LocalDateTime dateDebut, LocalDateTime dateFin)
            throws IOException, GeneralSecurityException {
        // En mode développement, retourner un lien fictif
      /**  if (devMode) {
            // Créer un ID de réunion fictif basé sur le timestamp
            String meetingId = "abc-defg-hij-" + System.currentTimeMillis();
            return "https://meet.google.com/" + meetingId;
        }**/

        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        Event event = new Event()
                .setSummary(titre)
                .setDescription(description)
                .setConferenceData(new ConferenceData()
                        .setCreateRequest(new CreateConferenceRequest()
                                .setRequestId(String.valueOf(System.currentTimeMillis()))
                                .setConferenceSolutionKey(new ConferenceSolutionKey()
                                        .setType("hangoutsMeet"))))
                .setStart(new EventDateTime()
                        .setDateTime(convertToDateTime(dateDebut)))
                .setEnd(new EventDateTime()
                        .setDateTime(convertToDateTime(dateFin)));

        Event createdEvent = service.events()
                .insert("primary", event)
                .setConferenceDataVersion(1)
                .execute();

        // Récupérer l'URL Meet du événement créé
        if (createdEvent.getConferenceData() != null &&
                createdEvent.getConferenceData().getEntryPoints() != null) {

            for (EntryPoint entryPoint : createdEvent.getConferenceData().getEntryPoints()) {
                if ("video".equals(entryPoint.getEntryPointType())) {
                    return entryPoint.getUri();
                }
            }
        }

        return null;
    }

    /**
     * Obtient les credentials pour l'API Google
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        if (credentialsFile == null || !credentialsFile.exists()) {
            throw new FileNotFoundException("Resource not found: credentials.json");
        }


        InputStream in = credentialsFile.getInputStream();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

// Dans la méthode getCredentials
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .setCallbackPath("/oauth2callback")
                .build();        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Convertit LocalDateTime en DateTime pour l'API Google
     */
    private DateTime convertToDateTime(LocalDateTime dateTime) {
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        return new DateTime(date);
    }
}