Deployment
===

Dit gaat het grote deployment-document worden. Maar momenteel zijn
er slechts enkele notes.

Builden
---

Ga naar de Telsysteem-map, en run `gradle installApp`. Dit maakt een
installatie-folder voor elk subproject in `subprojectnaam/build/install`.
Deze bevat dan een `bin`-dir met scriptjes om dit te runnen, en een
`lib`-dir met alle dependencies. Zie hiervoor dat Gradle
geïnstalleerd is. Als Gradle klaagt over "Unsupported major.minor version 52.0"
moet je ervoor zorgen dat Gradle draait met Java 8. Zet daarvoor
`export JAVA_HOME=/opt/jdk1.8.0` of iets dergelijks in `~/.bashrc`.

Reader
---

Op elke reader moet Redis staan. Zorg ervoor dat de append only file
in de Redis-configuratie aan staat, en elke seconde of bij elke update
synct. Elke seconde zou de beste balans tussen performance en veiligheid
van data zijn. Bij elke update is mogelijk al overkill.

We gebruiken [Embedded Java 8](http://www.oracle.com/technetwork/java/embedded/downloads/javase/index.html) (hardfloat voor ARM).

De java-executable die je moet gebruiken, staat na uitpakken in `ejdk1.8.0/linux_arm_vfp_hflt/jre/bin/java`.
