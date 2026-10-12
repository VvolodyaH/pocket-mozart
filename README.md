# Pocket Mozart 2.7

Little Mozart companion for android

[Scarica l’APK 2.7](https://github.com/VvolodyaH/pocket-mozart/raw/refs/heads/main/downloads/Pocket-Mozart-2.7.apk) · [Sorgenti completi in ZIP](https://github.com/VvolodyaH/pocket-mozart/raw/refs/heads/main/downloads/Pocket-Mozart-2.7-sorgenti.zip) · [Release e verifiche](https://github.com/VvolodyaH/pocket-mozart/releases/tag/v2.7.0)

L’APK è firmato con lo stesso certificato delle versioni precedenti: scegli **Aggiorna** durante l’installazione.

Widget Android nativo e trasparente con un piccolo Mozart in pixel art chubby. Ha 26 attività: passeggia, suona, compone, legge, beve un tè e si prende piccole pause. Sono inclusi 20 nuovi set di animazione, con 80 fotogrammi dedicati e notine musicali occasionali. Il personaggio viene disegnato dal launcher dentro il widget.

## Installazione e inserimento

Richiede Android 8.0 o successivo. L’APK è una build di sviluppo firmata, installabile da file.

1. Installa `Pocket-Mozart-2.7.apk`. Sopra una versione precedente, inclusa la 2.6, scegli **Aggiorna**: il pacchetto e la firma sono gli stessi.
2. Apri **Pocket Mozart** e premi **Aggiungi Mozart alla Home**. Conferma l’inserimento nel launcher.
3. Posiziona il widget e adatta i suoi bordi sulla Home. Mozart si avvia automaticamente.

Per evitare aperture accidentali, attiva **Blocca tocchi sul widget**, subito sotto i pulsanti di aggiunta/pausa. I tocchi su Mozart e sull’area del widget non aprono l’app; l’animazione continua. Il toggle si applica subito a tutti i widget inseriti e rimane memorizzato. Per cambiare le impostazioni o sbloccare i tocchi, apri Pocket Mozart dalla sua icona. Il blocco è disattivato per impostazione iniziale.

Non occorre concedere accessibilità, disegno sopra altre app o notifiche. Il servizio di accessibilità delle versioni precedenti è stato rimosso dall’app. Non c’è una schermata di configurazione obbligatoria né un editor separato dell’area.

Se il launcher non supporta il pulsante di aggiunta, scegli **Vai alla Home**, poi **Widget → Pocket Mozart**. Puoi aggiungerlo anche direttamente dal catalogo dei widget.

Lo spazio iniziale è 2×2 celle sui launcher Android 12+. Il widget si ridimensiona in entrambe le direzioni, fino ai limiti di una cella della griglia. Il launcher decide quando mostrare le maniglie e quali dimensioni consentire durante l’inserimento; su Samsung, se necessario, tieni premuto il widget. La griglia riserva la sua area, come per qualsiasi altro widget.

Tocca il widget per aprire l’app. **Metti Mozart in pausa** mostra una posa ferma; **Riprendi Mozart** riattiva le sequenze. Le impostazioni valgono per tutti i widget; ciascuno ha scelte casuali indipendenti.

## Animazioni e personalizzazione

| Attività | Animazione |
| --- | --- |
| Riposo | Busto e parte superiore salgono e scendono leggermente; piedi fermi |
| Passeggiata | Otto pose con gambe alternate, ginocchia piegate e piedi sollevati; si volta andando a sinistra |
| Spartito | Legge una partitura |
| Pianoforte | Suona un piccolo piano che compare nella sua area |
| Inchino | Saluto teatrale |
| Compleanno | Cappellino viola e oro, applausi e scintille |

Le **20 nuove attività** sono: violino, direzione d’orchestra, flauto, composizione con la penna, canticchiare, tè, lettura, lettera, orologio da tasca, sistemare la parrucca, minuetto, stiracchiarsi, sbadiglio, sonnellino, saluto, tricorno, rosa, carillon, valigia da viaggio e applauso. Ogni set contiene quattro fotogrammi distinti generati con il riferimento originale.

La camminata usa otto fotogrammi; le altre attività ne hanno quattro. I nuovi set riproducono i fotogrammi avanti e indietro, senza un salto fra l’ultimo e il primo. La fase dei passi segue la distanza percorsa. Tutto il fotogramma, inclusi piano e cappellino, rimane nei confini del widget.

La respirazione usa i pixel del personaggio originale: il busto sale di pochi pixel e torna alla posizione iniziale in un ciclo di due secondi. I piedi conservano esattamente gli stessi pixel in tutti i fotogrammi. La correzione del violino elimina il frammento isolato nei fotogrammi 1 e 3; i pixel del personaggio e dello strumento rimangono identici.

Il ciclo è sempre **idle → camminata verso un punto casuale → idle → attività casuale → idle**, e poi ricomincia. Mozart raggiunge la destinazione prima di fermarsi e mantiene una pausa visibile prima del gesto. I cambi di posa sono immediati, con il personaggio sempre opaco: le dissolvenze fra attività sono state rimosse.

La direzione dell’ultima passeggiata vale anche per idle, tutte le attività e i loro oggetti. Quando si muove verso sinistra, piano, spartito, cappellino e tutte le altre pose sono specchiati nella stessa cella, conservando posizione e dimensioni. La direzione cambia soltanto all’inizio di una nuova camminata con spostamento orizzontale. Quando l’area è completamente occupata dal personaggio, la fase di camminata avviene sul posto, con le gambe animate.

Le scelte nell’anteprima si applicano al prossimo turno di attività, dopo la passeggiata e il riposo. I tocchi ravvicinati sostituiscono la scelta in attesa. Anche il compleanno segue l’intero ciclo.

Ogni tanto compare una piccola nota dorata, rosa o lilla, anche in coppia. Sale per 2,4 secondi e svanisce. La casualità degli effetti è separata da quella dei percorsi; posizione e trasparenza delle note sono salvate nei checkpoint. Tutti gli effetti rimangono dentro la cella di Mozart, anche nelle aree basse.

Puoi scegliere la dimensione del personaggio da 64 a 160 dp, tre ritmi, il bordo opzionale e le date speciali. Mozart si riduce automaticamente nelle aree piccole. Il widget riproduce circa **8 fotogrammi/s** con risparmio energia attivo, **12 fotogrammi/s** disattivandolo. La fluidità effettiva dipende dal launcher e dal dispositivo. L’anteprima dentro l’app usa rispettivamente 12 e 24 fotogrammi/s.

Il **27 gennaio** compare periodicamente il cappellino. Il **30 settembre** e il **29 ottobre** il pianoforte è più frequente, per le prime del *Flauto magico* e del *Don Giovanni*. Il **5 dicembre** Mozart preferisce lo spartito. Si usa la data locale del telefono, aggiornata durante gli aggiornamenti standard del widget e quando cambi le impostazioni. I bottoni nell’anteprima permettono di provare tutte le attività in qualsiasi giorno.

Fonti storiche: [Fondazione Mozarteum](https://mozarteum.at/en/wolfgang-amade-mozart), [Il flauto magico nel catalogo Köchel](https://kv.mozarteum.at/en/work/die-zauberflote-the-magic-flute-singspiel-grand-opera-in-2-acts-7137?tab=structure), [edizione critica del Don Giovanni](https://dme.mozarteum.at/DME/objs/pdf/nma_68_-27_-3_eng.pdf).

## Come funzionano le sequenze casuali

Su **Android 12 e successivi**, la 2.7 prepara una sequenza completa nel launcher. Il lettore non deve più chiedere il prossimo fotogramma al servizio mentre torni nella pagina della Home: quel caricamento poteva applicare un'immagine vuota anche dopo la correzione della 2.5.

`ResidentTimeline` prepara percorsi e attività casuali usando lo stesso `MotionEngine`. L'ultima passeggiata riporta Mozart al punto iniziale; seguono idle e attività, poi la sequenza torna all'idle iniziale. Posizione, direzione e respirazione rimangono coerenti al raccordo. Le note finiscono prima della ripetizione; il personaggio resta sempre opaco. Nelle aree molto grandi il percorso viene limitato alla distanza percorribile nel tempo disponibile.

**La sequenza residente è finita e si ripete.** Il seme cambia con la data locale, quindi viene preparato un nuovo percorso al primo aggiornamento del giorno. Ogni widget ha un proprio seme. Le attività vengono pescate dall'intero catalogo: una singola sequenza può includerne soltanto una parte. Dimensioni e impostazioni possono cambiarne la durata: con una o due varianti dimensionali si tratta in genere di circa uno–tre minuti. Il limite complessivo è 1600 fotogrammi, suddiviso fra le varianti. Questo compromesso elimina le attese del servizio; la 2.6 proseguiva invece con nuove scelte senza un breve ciclo ripetuto.

Android 12/12L usa un unico `ViewFlipper` con figli stabili già installati nella vista; nessun timer annidato. Le immagini nascoste non vengono misurate a ogni cambio. Android 13+ usa `RemoteCollectionItems` e un `AdapterViewFlipper`: tutti gli elementi sono già residenti, mentre le viste vengono create quando servono. Questa distinzione evita la serializzazione ripetuta dei metadati su Android 12. I layout hanno identificatori distinti dalla 2.6, per sostituire il vecchio lettore durante l'aggiornamento.

Su Android 8–11 rimane `WidgetFramesService`, con blocchi immutabili di circa quattro secondi, cache limitata e checkpoint. Conserva la correzione della 2.5 contro l'invalidazione superflua alla ripresa, ma può ancora mostrare il caricamento trasparente quando il launcher non ha il prossimo fotogramma. La nuova eliminazione delle attese riguarda Android 12+.

Il blocco dei tocchi conserva posizione e fotogramma anche durante la riapplicazione. Su Android 12 vengono riutilizzati i figli con ID stabile; sulle altre versioni viene mantenuto l'adapter. Le pose sono specchiate nella propria cella e le immagini rimangono trasparenti, con filtro pixel disattivato.

Sono fornite fino a quattro varianti dimensionali richieste dal launcher; quando ne comunica di più si usa la coppia nativa per orientamento. Il ridimensionamento prepara una sequenza per la nuova area. Un aggiornamento ordinario nella stessa giornata, senza cambi di configurazione, conserva i fotogrammi numerati e la posizione corrente. Cambiare area, ritmo, dimensione del personaggio, modalità di riproduzione o giorno può avviare un percorso nuovo. Dopo il riavvio completo del launcher il ripristino dell'indice dipende dal launcher.

Il widget appartiene alla sua pagina della Home. La finestra nascosta sospende il timer; il launcher può applicare politiche proprie alle pagine fuori schermo. L'app funziona offline, senza account, analytics, permessi overlay o accessibilità. L'aggiornamento periodico rimane quello nativo, ogni 30 minuti. Non viene usato un servizio in primo piano.

## Compilazione

Apri il progetto in Android Studio oppure usa JDK 17 e Android SDK 35:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

L’APK è `app/build/outputs/apk/debug/app-debug.apk`. Configura `ANDROID_HOME` o il tuo `local.properties`, escluso dai sorgenti distribuiti. Il progetto usa Java 17, Gradle 8.10.2 e Android Gradle Plugin 8.7.3, senza librerie runtime esterne.

## Asset e codice

- `app/src/main/assets/sprites/mozart.png`: attività, atlante RGBA 1024×1536, celle 256×256.
- `app/src/main/assets/sprites/mozart-walk.png`: otto pose della passeggiata, RGBA 1024×512.
- `app/src/main/assets/sprites/mozart-extra-0.png` … `mozart-extra-3.png`: quattro atlanti da 1024×1280 per i nuovi set; dimensioni adatte anche alle texture dei dispositivi meno recenti.
- `app/src/main/res/drawable-nodpi/sprite_*.png`: 116 risorse native, incluse le otto pose rivolte a sinistra; pixel identici alle celle degli atlanti.
- `app/src/main/res/drawable/pixel_note_*.xml`: tre simboli musicali a pixel, trasparenti.
- `art/mozart-extra.png`: atlante completo degli 80 fotogrammi nuovi, per esportazione e anteprima.
- `art/catalogo.png`: tavola con tutte le 26 attività.
- `art/reference.jpg` e `art/mozart*-generated.png`: riferimento e immagini generate prima dell’importazione.
- `art/anteprima.html`: anteprima offline degli sprite nel browser. Permette di provare pose e dimensioni; non simula il servizio o la cache del launcher Android.
- `ResidentTimeline.java`, `MotionEngine.java`, `WidgetTimeline.java` e `WidgetClipStore.java`: movimento, fotogrammi numerati, date, cache limitata e checkpoint.
- `NativeWidgetRenderer.java`, `PixelSprites.java` e `WidgetFramesService.java`: costruzione e consegna dei fotogrammi nativi.
- `MozartWidgetProvider.java`: aggiunta, aggiornamento e ridimensionamento.
- `PetSceneView.java` e `SpriteAtlas.java`: anteprima dentro l’app.

Per rigenerare le risorse native a partire dagli atlanti inclusi:

```bash
java -Djava.awt.headless=true tools/BakeSpriteFixes.java .
java -Djava.awt.headless=true tools/ExportWidgetFrames.java .
python3 tools/validate_assets.py
python3 tools/make_preview.py
```

Gli importatori `CompileSprites.java`, `CompileWalkSprites.java` e `CompileExtraSprites.java` conservano la preparazione degli atlanti a partire dalle immagini generate. Dopo l’importazione, `BakeSpriteFixes.java` prepara i fotogrammi della respirazione dai pixel originali e applica soltanto la zona trasparente della correzione generata per il violino. Il rig mantiene fermi i pixel dei piedi e usa nearest neighbor per il movimento della parte superiore. La patch è riapplicata anche da `CompileExtraSprites.java`, così l’importazione non reintroduce il frammento. Il file `art/mozart-idle-reference.png` conserva il riferimento del rig; gli originali dei due fotogrammi sporchi e l’immagine generata di correzione consentono di verificare la riparazione.

## Verifica

Compilazione, test JUnit/Robolectric e lint sono descritti in `VERIFICA.md`. I test usano le API 26, 31 e 35, i due lettori residenti e la cache del lettore precedente, e verificano geometria, continuità, risorse, aggiunta e migrazione. Le transizioni del launcher Samsung reale, il consumo e la memoria sul dispositivo richiedono una prova sul telefono.
