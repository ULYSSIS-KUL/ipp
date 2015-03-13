Het hoog niveau ontwerp van de 24urenloop software
===

Hardware
---

We gebruiken de volgende componenten:

- 3 x telpunt, met:
    - Impinj speedrunner lezer met antenne
    - Raspberry Pi
    - Switch
- 2 x laptop

Redis
---

We gebruiken [Redis](http://redis.io) voor het registreren en ophalen van events. Ophalen, niet doorsturen: het is een pull-systeem.

- Telpunt: 2 x 2 queues (1 paar van queues per laptop)
    - pendingN (met N het laptopnummer)
    - processedN
- Laptop:
    - snapshots

- Telpunt kan ook messages broadcasten, voor status updates.

Telpunten
---

De taak van de telpunten is: registreer tags die voorbij de antenne komen en
plaats deze **atomair** op de pendingN queues, en verhoog de update count:

```redis
MULTI

LPUSH pending1 <update als JSON, met huidige update_count als id>
LPUSH pending2 <update als JSON, met huidige update_count als id>
INCR update_count

EXEC
```

`update_count` + id van het telpunt identificeren een update uniek.

Restore: enkel de update\_count dient opgehaald te worden.

```redis
GET update_count
```

Als deze niet bepaald is, is deze 0.

Laptops
---

De taak van de laptops is het bijhouden van de stand, en deze desnoods te corrigeren.

Ophalen van de updates: 1 thread per telpunt. Deze thread monitort het telpunt, en haalt de updates op:

```redis
BRPOPLPUSH pendingN processedN
```

Regelmatig worden snapshots genomen:

```redis
LPUSH snapshots <snapshot als JSON, met hoogste update_count voor elk telpunt>
```

Restore: laad de laatste snapshot, kijk naar de laatste update van de processed queue:

```redis
LINDEX processedN 0
```

Als deze kleiner is dan de update count in de snapshot, ga dan verder met het verwerken van updates.

Als deze groter is, neem dan alle N laatste updates van de lijst, waar N gelijk is aan het verschil tussen
de laatste update in de snapshot en de laatste update in `processedN`. Verwerk deze in tegenovergestelde volgorde.

```redis
LRANGE processedN 0 N-1
```

Ga daarna verder met het verwerken van updates zoals normaal.

Correctieronde: Snapshot nemen op beide laptops? Hier ben ik nog niet zeker over.

Server die stand toont
---

Laptops hebben een reverse SSH-tunnel naar de server, zodat de server met de laptops kan verbinden. De
server haalt periodiek de stand op. Deze probeert altijd eerst de eerste laptop, als deze niet reageert,
gebruikt de server de tweede laptop. Hoe wordt de stand bepaald? Laatste snapshot? Hoe vaak snapshotten?
Kunnen we dat altijd doen?
