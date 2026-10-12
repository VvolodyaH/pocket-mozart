# 2.7.0

- Su Android 12+, riproduzione interamente residente: eliminata la richiesta asincrona del fotogramma che poteva mostrare il caricamento trasparente al ritorno sulla Home, anche senza il vecchio aggiornamento superfluo della cache.
- Android 12/12L: un solo ViewFlipper con figli stabili; Android 13+: RemoteCollectionItems nativi. Layout distinti per rimuovere il lettore precedente all'aggiornamento.
- Percorso casuale chiuso con una vera passeggiata di ritorno, conservando ciclo idle → camminata → idle → attività, direzione delle pose e note. Nessuna dissolvenza del personaggio.
- La sequenza finita si ripete e cambia ogni giorno. La durata dipende dalle varianti dimensionali e dal ritmo; non tutte le attività devono comparire in ogni singola sequenza.
- Budget complessivo di 1600 fotogrammi, varianti responsive limitate e fallback per orientamento; verifica del Parcel sotto 800 KiB. Android 12 evita i metadati ripetuti della collezione e non misura i figli nascosti.
- Blocco tocchi e riapplicazioni conservano il fotogramma corrente. Android 8–11 mantiene il servizio e i checkpoint precedenti.
- Nuove regressioni sulle API 31 e 35: ritorni da pagina nascosta, schermo spento simulato tramite visibilità, ripetizione completa, risposta immediata senza servizio, payload, riapplicazione e tocchi su Android 12. Controllo che riproduce il vuoto nel lettore precedente senza invalidazione programmata dall'app.
- VersionCode 10; stesso pacchetto e certificato per aggiornare la 2.6.

Limiti e risultati automatici in `VERIFICA.md`. Non provato sul launcher Samsung fisico.

# 2.6.0

- Aggiunto il toggle **Blocca tocchi sul widget** vicino ai pulsanti principali, con preferenza persistente e aggiornamento immediato di tutti i widget inseriti.
- Il blocco impedisce l’apertura dell’app sia toccando Mozart sia toccando l’area del widget, anche quando è in pausa. Le impostazioni restano accessibili dall’icona dell’app.
- Azioni dei tocchi aggiornate anche su viste e fotogrammi già caricati; adapter, posizione e sequenza restano gli stessi durante blocco e sblocco.
- Evento senza azioni per i tocchi della collezione bloccata, compatibile con i vecchi handler Android che non gestiscono un template nullo.
- Sei nuovi casi Robolectric sulle API 26 e 35: tocco sullo sfondo, fotogrammi già caricati e successivi, blocco/sblocco, widget in pausa, persistenza e due widget aggiornati dal toggle reale.
- VersionCode 9, stesso pacchetto e certificato per l’aggiornamento. Sequenza e checkpoint compatibili con 2.4 e 2.5.

Verifica automatica e limiti della prova sul telefono in `VERIFICA.md`.

# 2.5.0

- Corretto un aggiornamento superfluo della collezione dopo la ricreazione del servizio: poteva eliminare il prossimo fotogramma già precaricato e mostrare il caricamento trasparente alla riaccensione dello schermo.
- Avvio del precaricamento limitato ai primi cinque fotogrammi; una richiesta di un indice avanzato annulla l’aggiornamento, anche se era stato programmato da una richiesta iniziale dell’indice zero.
- Annullamento definitivo del callback alla distruzione della factory, con sincronizzazione fra caricamento e callback.
- Dieci nuovi casi Robolectric sulle API 26 e 35: cache e flipper reali, servizio ricreato, ripristino dopo indice zero, avvio iniziale a entrambi i ritmi, distruzione e controllo del fotogramma trasparente dopo invalidazione. Quattro regressioni fallivano prima della correzione.
- Identità della sequenza e checkpoint compatibili con la 2.4: ciclo, pose, respirazione, violino, specchio e note conservati.
- VersionCode 8, stesso pacchetto e certificato per l’aggiornamento.

La scomparsa è riprodotta simulando la ricreazione del servizio e l’invalidazione della cache nel framework Android; la verifica sul launcher Samsung fisico resta da eseguire. Dettagli in `VERIFICA.md`.

# 2.4.0

- Idle con respirazione: busto e parte superiore salgono e scendono di pochi pixel in un ciclo di due secondi; piedi fermi.
- Fotogrammi ottenuti con un rig dei pixel originali, mantenendo stile e proporzioni del personaggio.
- Rimosso il frammento isolato fuori dal violino nei fotogrammi 1 e 3, applicando soltanto la zona trasparente della correzione generata. Personaggio e strumento mantengono tutti i loro pixel originali.
- Verifiche su piedi invariati, movimento del busto, assenza di frammenti scollegati in tutti i fotogrammi del violino e riparazione limitata al difetto.
- Conservati ciclo idle → passeggiata → idle → attività, cambi immediati, specchio di tutte le pose, notine e continuità nel widget 3×1.
- VersionCode 7, stesso pacchetto e certificato per l’aggiornamento.

Verifica automatica e limiti della prova sul telefono in `VERIFICA.md`.

# 2.3.0

- Eliminate le dissolvenze tra le animazioni; ogni posa resta opaca.
- Ciclo obbligatorio: idle, passeggiata verso un punto casuale, idle, attività casuale, ritorno all’idle e ripartenza.
- Destinazione raggiunta prima del riposo, con continuità fra i blocchi del widget.
- Tutte le pose e i loro oggetti si specchiano dopo una passeggiata verso sinistra, fino alla camminata successiva; lo specchio mantiene la cella nella stessa posizione.
- Anche compleanno e scelte nell’anteprima rispettano il ciclo. I tocchi rapidi aggiornano il gesto in attesa.
- Nelle aree completamente occupate dal personaggio, la fase di camminata anima i passi sul posto.
- Conservate le 26 attività, le notine musicali, i checkpoint e le correzioni contro i teletrasporti nei widget 3×1.
- VersionCode 6, stesso pacchetto e certificato per l’aggiornamento.

Verifica automatica e limiti della prova sul telefono in `VERIFICA.md`.

# 2.2.0

- Aggiunti 20 set originali di animazione, con 80 fotogrammi generati sul riferimento Mozart: 26 attività complessive.
- Violino, direzione d’orchestra, flauto, penna e canticchiare; tè, libro, lettera, orologio e parrucca; minuetto, stiracchiarsi, sbadiglio, sonnellino e saluto; tricorno, rosa, carillon, viaggio e applauso.
- Nuove animazioni in sequenza avanti/indietro e sprite registrati sulla stessa cella, con piedi allineati e margini trasparenti.
- Passaggi di mezzo secondo in entrata e uscita e idle visibile fra tutte le attività, compresi compleanno e selezione manuale.
- Notine musicali occasionali, anche in coppia, con salita e dissolvenza dentro i confini del personaggio.
- Anteprima con tutti i 26 pulsanti: scegliere un’attività riporta alla scena visibile; i tocchi ravvicinati aggiornano la scelta in attesa.
- Conservati fotogrammi immutabili, adapter stabile, checkpoint e correzioni contro i salti nel widget 3×1.
- VersionCode 5, stessa firma e pacchetto per aggiornare la versione precedente.

Verifica automatica e limiti del controllo sul telefono in `VERIFICA.md`.

# 2.1.0

- Corretti i salti di posizione nei widget larghi e bassi, inclusa l’area 3×1.
- Rimossa la posa fissa al centro dalla vista temporanea di caricamento.
- Fotogrammi numerati e immutabili: le richieste anticipate o fuori ordine non cambiano la sequenza in riproduzione.
- Seme e identità dell’adapter conservati durante gli aggiornamenti ordinari del widget.
- Cache limitata e checkpoint per ricostruire la stessa passeggiata dopo la ricreazione del servizio.
- Conservati il timer nativo Android, lo sfondo trasparente, gli otto passi e l’assenza di permessi overlay/accessibilità.
- 42 test superati; le sei regressioni sui salti fallivano nella 2.0 e passano nella 2.1.

La verifica sul launcher Samsung reale resta da eseguire con l’APK aggiornato. Dettagli in `VERIFICA.md`.

# 2.0.0

- Widget nativo trasparente: Mozart viene riprodotto dal launcher dentro la propria area.
- Rimossi overlay, servizio di accessibilità, servizio in primo piano e tutti i permessi richiesti.
- Sequenze casuali preparate in blocchi di circa 45 secondi, con continuità dello stato e nuove scelte al blocco successivo.
- Riproduzione nativa a circa 8 o 12 fotogrammi/s, con precaricamento dei fotogrammi vicini e transizioni immediate.
- Conservate le otto pose della passeggiata, le altre attività e le date speciali; 36 risorse trasparenti native.
- Varianti responsive per le dimensioni comunicate dal launcher, senza configurazioni obbligatorie.
- Migrazione dalle versioni precedenti e stesso pacchetto/certificato per l’aggiornamento.

La fluidità e il collegamento del servizio nel launcher Samsung restano da verificare sul telefono. Dettagli in `VERIFICA.md`.

# 1.1.0

- Camminata rigenerata in otto pose: gambe alternate, ginocchia piegate, piedi sollevati e appoggio a terra. La sequenza segue la distanza percorsa, il ritmo e la dimensione del personaggio.
- Un solo comando di aggiunta alla Home, con avvio del servizio dall’app visibile e ritorno alla Home dopo l’inserimento. Rimossa la configurazione obbligatoria del widget.
- Widget iniziale 2×2, ridimensionabile in entrambe le direzioni fino ai limiti di una cella del launcher. Anteprima con il personaggio reale.
- Rimossi editor e override dell’area manuale, compresi quelli salvati dalla 1.0. Posizione e dimensioni seguono esclusivamente il widget.
- Eventi di movimento/ridimensionamento raggruppati senza rinviare continuamente la scansione.
- Annullare l’aggiunta senza alcun widget interrompe il servizio predisposto per l’inserimento.
- 20 test superati, compilazione e lint completati. Firma uguale alla 1.0.

Il launcher controlla la griglia e quando mostrare le maniglie del widget. La nuova procedura Samsung va verificata sul telefono; i test Android sono eseguiti con Robolectric sulle API 26 e 35.
