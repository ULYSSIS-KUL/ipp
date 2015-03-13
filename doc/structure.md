Structuur
===

Het project is als volgt opgedeeld:

- `org.ulyssis.countingsystem`: root package voor het telsysteem.
    - `Main`: toegangspunt om uit te voeren, bevat enkel main-methode.
    - `config`: bevat klassen die betrekking hebben tot de configuratie van het systeem.
        - `Config`: configuratie, gelezen uit JSON, onderdelen uit deze configuratie
                    staan ook in `*Config`, zoals `ProcessorConfig` en `ReaderConfig`.
        - `Options`: command line opties.
    - `messages`: bevat alle mogelijke messages die naar JSON geserialiseerd worden
                  en gecommuniceerd over Redis.
    - `reader`: bevat de code die betrekking heeft tot de reader, een reader
                komt overeen met een Raspberry Pi, die updates ontvangt via LLRP
                en deze op Redis zet.
        - `Reader`: hoofdklasse van de werkelijke reader
        - `StandardLLRPReader`: bevat alle LLRP-specifieke stuff. Dit is een basic
                                implementatie waar je van kan overerven en messageReceived()
                                implementeren.
