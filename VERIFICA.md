# Verifica · Pocket Mozart 2.7.0

Eseguita il 12 ottobre 2026.

## Risultato

- `:app:assembleDebug`: APK 2.7.0, versionCode 10, pacchetto `it.mozart.pocket`, minSdk 26 e targetSdk 35. Dimensione: 14.504.588 byte.
- `:app:testDebugUnitTest`: **76 test, zero fallimenti, errori o test saltati**. 28 test JUnit e 48 casi Robolectric sulle API 26, 31 e 35.
- `:app:lintDebug`: **zero errori**, sei avvisi già presenti: tre attributi per Android recenti, una regola di backup e due duplicazioni intenzionali delle immagini.
- Firma verificata con `apksigner`, identica alla 2.6 e alle versioni precedenti. SHA-256 del certificato: `62ef238df8c9a7b830f4cff8c2da1716a91478869eefa399d6950e89f0fe3ba5`.
- Manifest senza permessi richiesti. Il servizio `BIND_REMOTEVIEWS` rimane per Android 8–11; Android 12+ usa i fotogrammi residenti.
- Sprite e atlanti invariati rispetto alla 2.6; conservati i confronti grafici nativi per tutti i 108 fotogrammi delle 26 pose.

## Causa riprodotta

`AdapterViewFlipper` passa immediatamente all'elemento richiesto. Se `RemoteViewsAdapter` non lo trova nella cache, applica la vista di caricamento trasparente mentre aspetta il servizio. Basta un fotogramma mancante: non è necessario il callback superfluo corretto nella 2.5.

Il controllo `thePreviousServicePlayerCanBlankWithoutAnyWarmupInvalidation`, sulle API 31 e 35, prepara il vecchio flipper e la cache Android con i fotogrammi 400 e 401, poi richiede un indice non ancora caricato. Senza chiamare l'invalidazione della nostra app, il framework mostra un elemento privo dell'immagine del personaggio. Questo dimostra il meccanismo compatibile con la segnalazione; non identifica mediante log la causa esatta sul Samsung fisico.

## Correzione su Android 12+

I fotogrammi vengono consegnati tutti prima della riproduzione. Android 12/12L usa un solo `ViewFlipper`, con figli stabili e senza animazioni di entrata/uscita. Android 13+ usa `RemoteCollectionItems` con `AdapterViewFlipper`. Entrambi i percorsi eliminano il servizio remoto e la sua vista di caricamento dalla riproduzione.

Il percorso Android 12 evita un problema distinto: quella versione serializza ripetutamente i metadati dell'app per ciascun elemento di `RemoteCollectionItems`. Una prova con 1600 elementi raggiungeva circa 3,55 MB. I figli di `RemoteViews` condividono invece i dati della gerarchia. Il budget complessivo è di 1600 fotogrammi fra tutte le varianti e i test controllano l'intero Parcel sotto 800 KiB, lasciando margine rispetto al limite Binder di 1 MiB. Non vengono trasferite bitmap per ciascun fotogramma, ma riferimenti alle risorse incluse nell'APK.

I nuovi layout hanno ID da `0x7f040005` a `0x7f040008`, distinti dai precedenti layout animati `0x7f040000` e `0x7f040004`: l'aggiornamento può sostituire la vecchia vista e abbandonare i suoi callback del servizio. Il normale aggiornamento della stessa configurazione conserva l'adapter o i figli stabili e l'indice corrente.

## Continuità e compromesso

La sequenza residente è finita: si ripete e cambia con la data locale al primo aggiornamento del giorno. Una singola sequenza sceglie una parte delle attività dal catalogo completo. Con una o due varianti dimensionali dura generalmente circa uno–tre minuti; più varianti riducono il tempo disponibile. Android 8–11 mantiene il lettore precedente, senza il nuovo ciclo finito ma con la possibilità di caricamenti trasparenti.

La chiusura non riporta istantaneamente Mozart al centro: l'ultima passeggiata raggiunge il punto iniziale, poi seguono idle e attività, prima dell'idle della ripetizione. La direzione dell'idle iniziale è coerente con quella finale. Le note terminano prima del raccordo; non viene modificata l'opacità del personaggio. Per aree molto grandi, la distanza del percorso è limitata dal tempo a disposizione.

`ResidentTimelineTest` verifica i due ritmi (83 e 125 ms), tre budget (400, 800 e 1600), quattro geometrie (300×56, 40×40, 64×240, 2000×2000) e otto semi: **192 combinazioni**. Ogni coppia di fotogrammi, compreso ultimo→primo, rispetta velocità, confini, ordine idle→walk→idle→attività e direzione. Sono controllati note, respirazione iniziale, riproducibilità, catalogo completo su cinquanta semi e compleanno.

## Ripresa, ridimensionamento e tocchi

`ResidentPlaybackTest` serializza e ricostruisce i RemoteViews prima di applicarli nell'host. Su API 31 e 35, per entrambi i ritmi, percorre tutti i fotogrammi e supera il raccordo. Ogni 53 fotogrammi nasconde la finestra per sei secondi simulati e la rende visibile: l'indice resta fermo durante la pausa, la vista corrente è visibile e opaca e contiene subito il drawable. Nessun callback di caricamento viene completato per rendere disponibile il prossimo frame.

Su API 31 viene eseguito il vero `ViewAnimator`: lo shadow standard di Robolectric mantiene un indice separato dal campo nativo letto alla ripresa, quindi non rappresenta correttamente questo caso. Su API 35 viene controllata anche l'assenza di un `RemoteViewsAdapter` legato al servizio.

La verifica del trasferimento usa una, due, quattro e sedici dimensioni comunicate dal launcher. Oltre quattro, il renderer adotta la coppia per orientamento. Le geometrie della sequenza restano entro l'area ricevuta, compresa la riga 3×1.

Il test dei tocchi Android 12 monta la vista in un'Activity, invia eventi down/up sul personaggio e controlla apertura, blocco e sblocco. L'indice 160 e lo stesso figlio visibile sopravvivono alla riapplicazione; anche il fotogramma successivo resta bloccato. Lo sblocco conserva l'indice 161 e ripristina l'apertura. I test precedenti su API 26/35 coprono template della collezione, sfondo, widget in pausa, persistenza e due widget aggiornati dal toggle dell'interfaccia.

Restano i test di movimento, checkpoint legacy, mirroring pixel per pixel di tutte le pose, respirazione, note senza residui, avvio e aggiunta alla Home. Le cinque regressioni della correzione 2.5 restano sul percorso legacy API 26.

## Limiti della verifica

**Non provato su un telefono Samsung fisico.** Robolectric verifica il framework e la serializzazione, ma non riproduce il compositing, i gesti, la gestione della memoria o le politiche del launcher Samsung. La sospensione è simulata attraverso la visibilità della finestra; non è uno spegnimento fisico del display.

La nuova riproduzione elimina l'attesa del servizio. Non può impedire al launcher di ricreare interamente una pagina o sospendere il proprio rendering. Android 12 mantiene più viste in memoria rispetto al lettore con collezione di Android 13+: memoria, tempi del primo inserimento, consumo e fluidità sotto carico vanno controllati sul dispositivo.

Per la prova: installa scegliendo **Aggiorna**, conserva il widget 3×1 e passa più volte dalla pagina di destra a quella di Mozart. Prova anche spegnimento/sblocco e apertura/chiusura delle app, entrambi i ritmi e il blocco dei tocchi. Osserva almeno una ripetizione completa: il ritorno al punto iniziale deve avvenire camminando. Se il launcher mantiene una vista precedente dopo l'aggiornamento, pausa e ripresa dall'app ne richiedono la sostituzione.
