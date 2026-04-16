# IMDb Real-Time Streaming Pipeline

A real-time data pipeline that ingests actor and news data from the **IMDb Alternative API**, streams it through **Apache Kafka**, processes it with **Apache Spark Streaming**, and persists it in **Apache Cassandra**.

---

## Table of Contents

- [Architecture](#architecture)
  - [Pattern](#pattern)
  - [Data Flow](#data-flow)
  - [Architecture Diagram](#architecture-diagram)
  - [Architectural Improvements](#architectural-improvements)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Modules](#modules)
  - [kafka-producer](#kafka-producer)
  - [spark-processor](#spark-processor)
- [Data Model](#data-model)
- [Kafka Topics](#kafka-topics)
- [Configuration](#configuration)
- [Setup & Run](#setup--run)
- [Roadmap](#roadmap)

---

## Architecture

### Pattern

This project implements a **Kappa Architecture** — a simplification of Lambda Architecture that removes the batch layer and relies exclusively on a real-time stream processing layer.

| Lambda Architecture | Kappa Architecture (this project) |
|---|---|
| Batch Layer + Speed Layer + Serving Layer | Speed Layer + Serving Layer only |
| Reprocesses historical data from a data lake | Replays from Kafka log (limited retention) |
| Higher complexity | Simpler, fewer moving parts |

The three layers of this pipeline:

| Layer | Technology | Role |
|---|---|---|
| **Speed Layer** | Kafka + Spark Streaming | Real-time ingestion and processing |
| **Serving Layer** | Apache Cassandra | Low-latency reads for downstream consumers |
| **Source** | IMDb Alternative API (RapidAPI) | External data provider |

---

### Data Flow

```
IMDb Alternative API
        │
        │  HTTP (Unirest)
        ▼
┌───────────────────┐
│   kafka-producer  │
│                   │
│  ActorProducer ───┼──► ActorTopic ──────────────────────────┐
│  NewsProducer  ───┼──► NewsTopic  ──────────────────────┐   │
│                   │                                     │   │
└───────────────────┘                                     │   │
                                                          │   │
                                              ┌───────────▼───▼────────┐
                                              │    Apache Kafka        │
                                              │    (Message Broker)    │
                                              └───────────┬───┬────────┘
                                                          │   │
                                          ┌───────────────▼─┐ └──────────────────┐
                                          │                 │                    │
                                 ┌────────▼─────────┐  ┌───▼──────────────────┐ │
                                 │ NewsStreamConsumer│  │ ActorStreamConsumer  │ │
                                 │                   │  │                      │ │
                                 │ Text Processing   │  │  JSON Deserialize    │ │
                                 │ (Word Frequency / │  │  → Cassandra Insert  │ │
                                 │  Tag Extraction)  │  │                      │ │
                                 └────────┬──────────┘  └──────────┬───────────┘ │
                                          │                        │             │
                                          └────────────┬───────────┘             │
                                                       │                         │
                                              ┌────────▼────────┐                │
                                              │  Apache Cassandra│                │
                                              │  imdb_keyspace2  │                │
                                              │                  │                │
                                              │  actors          │◄───────────────┘
                                              │  newsTable       │
                                              │  movies          │
                                              │  topRatedMovies  │
                                              │  actorMovies     │
                                              └──────────────────┘
```

---

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SPEED LAYER                                   │
│                                                                      │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────────────┐  │
│  │             │    │                  │    │                    │  │
│  │  IMDb API   │───►│  kafka-producer  │───►│   Apache Kafka     │  │
│  │  (RapidAPI) │    │                  │    │                    │  │
│  │             │    │  - ActorProducer │    │  Topics:           │  │
│  └─────────────┘    │  - NewsProducer  │    │  - ActorTopic      │  │
│                     │                  │    │  - NewsTopic       │  │
│                     └──────────────────┘    │                    │  │
│                                             └────────┬───────────┘  │
│                                                      │               │
│                                             ┌────────▼───────────┐  │
│                                             │  spark-processor   │  │
│                                             │                    │  │
│                                             │  ActorStream-      │  │
│                                             │  Consumer          │  │
│                                             │                    │  │
│                                             │  NewsStream-       │  │
│                                             │  Consumer          │  │
│                                             │  + Tag Extraction  │  │
│                                             └────────┬───────────┘  │
└──────────────────────────────────────────────────────┼──────────────┘
                                                       │
┌──────────────────────────────────────────────────────▼──────────────┐
│                        SERVING LAYER                                 │
│                                                                      │
│                    ┌─────────────────────┐                          │
│                    │   Apache Cassandra  │                          │
│                    │   imdb_keyspace2    │                          │
│                    └─────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────┘
```

---

### Architectural Improvements

The following improvements are recommended to move this pipeline toward production-readiness:

#### 1. Evolve to Full Lambda Architecture
Add a **batch layer** to handle historical data and fault recovery:

```
┌─────────────────────────────────────────────────────────────────────┐
│  BATCH LAYER (add)                                                   │
│  Raw data lake (S3 / HDFS) + Apache Spark batch jobs                │
│  → Scheduled reprocessing of full history                           │
└─────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────┐
│  SPEED LAYER (current)                                               │
│  Kafka + Spark Streaming                                             │
└─────────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────────┐
│  SERVING LAYER (current)                                             │
│  Cassandra — merge batch view + real-time view                      │
└─────────────────────────────────────────────────────────────────────┘
```

#### 2. Add Schema Registry
Replace plain JSON strings in Kafka with **Avro schemas** validated by a Confluent Schema Registry. Enforces a contract between producer and consumer and enables schema evolution.

#### 3. Add a Dead-Letter Topic
Failed messages currently disappear silently. Route them to a `DeadLetterTopic` for replay or manual inspection:
```
Processing failure → DeadLetterTopic → Alert / Replay
```

#### 4. Upgrade to Spark Structured Streaming (Spark 3.x)
The current Spark Streaming DStream API is deprecated. Structured Streaming provides:
- Exactly-once semantics
- SQL-like DataFrame API
- Better Kafka integration (`spark-sql-kafka`)
- Automatic micro-batch optimization

#### 5. Add a REST API Serving Layer
Cassandra data is not currently exposed. Add a **Spring Boot REST API** in front of Cassandra:
```
Client → REST API (Spring Boot) → Cassandra
```

#### 6. Add Observability
| Concern | Tool |
|---|---|
| Kafka metrics | Kafka Exporter + Prometheus |
| Spark metrics | Spark UI + Prometheus sink |
| Cassandra metrics | Cassandra Exporter |
| Dashboards | Grafana |
| Distributed tracing | OpenTelemetry |

#### 7. Containerize with Docker Compose
Provide a `docker-compose.yml` for one-command local startup:
```yaml
services: [zookeeper, kafka, cassandra, kafka-producer, spark-processor]
```

---

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 8 | Application language |
| Apache Kafka | 3.7.0 | Distributed message broker |
| Apache Spark Streaming | 1.6.2 | Real-time stream processing |
| Apache Cassandra | 3.x | Distributed NoSQL database |
| Cassandra Java Driver | 3.11.5 | Cassandra client |
| Unirest | 1.4.9 | HTTP client for REST API calls |
| Jackson Databind | 2.17.2 | JSON serialization |
| OpenCSV | 5.9 | CSV parsing |
| SLF4J + Logback | 2.0.13 | Structured logging |
| Maven | 3.x | Build tool |

---

## Project Structure

```
imdb-streaming-pipeline/
│
├── kafka-producer/                  # Maven module — data ingestion
│   └── src/main/java/
│       ├── ProducerApplication.java # Entry point
│       ├── ActorProducer.java       # Kafka producer — actor events
│       ├── NewsProducer.java        # Kafka producer — news events (main loop)
│       ├── api/
│       │   ├── ImdbApiClient.java   # HTTP client with retry + key rotation
│       │   ├── DataSeedRunner.java  # Utility to seed static CSV data
│       │   ├── actor/
│       │   │   ├── ActorBiographyClient.java    # GET /actors/get-bio
│       │   │   ├── ActorBornTodayClient.java    # GET /actors/list-born-today
│       │   │   ├── ActorNewsClient.java         # GET /actors/get-all-news
│       │   │   ├── ActorKnownForClient.java     # GET /actors/get-known-for
│       │   │   ├── FilmographyClient.java       # GET /actors/get-all-filmography
│       │   │   └── PopularCelebsClient.java     # GET /actors/list-most-popular-celebs
│       │   └── movie/
│       │       ├── MovieMetadataClient.java     # GET /title/get-meta-data
│       │       ├── MovieRatingClient.java       # GET /title/get-ratings
│       │       ├── TopRatedMoviesClient.java    # GET /title/get-top-rated-movies
│       │       ├── SimilarMoviesClient.java     # GET /title/get-more-like-this
│       │       ├── ComingSoonTvShowsClient.java # GET /title/get-coming-soon-tv-shows
│       │       └── PopularGenresClient.java     # GET /title/list-popular-genres
│       ├── util/
│       │   ├── Actor.java        # Actor domain model
│       │   ├── Movie.java        # Movie domain model
│       │   ├── News.java         # News domain model
│       │   ├── SimpleDate.java   # Date wrapper (month/day comparison)
│       │   └── AppConfig.java    # config.properties loader
│       └── work/
│           ├── ApiKeyRotator.java # Rotates API keys on rate limit
│           └── NewsGetter.java    # Legacy news fetching utility
│
├── spark-processor/                 # Maven module — stream processing
│   └── src/main/java/
│       ├── ActorStreamConsumer.java # Spark job — ActorTopic → Cassandra
│       ├── NewsStreamConsumer.java  # Spark job — NewsTopic → tag extraction → Cassandra
│       └── cassandra/
│           ├── SchemaInitializer.java  # Entry point — creates keyspace + tables + seeds data
│           ├── CassandraConnector.java # Cluster connection (AutoCloseable)
│           ├── KeyspaceCreator.java    # CREATE KEYSPACE
│           ├── TableCreator.java       # CREATE TABLE
│           └── CsvDataLoader.java      # Bulk load from CSV files
│
├── .gitattributes
└── README.md
```

---

## Modules

### kafka-producer

Responsible for all data ingestion from the IMDb Alternative API.

**Real-time flow (streaming):**
1. `ProducerApplication` starts `NewsProducer` in a dedicated thread with a JVM shutdown hook
2. `NewsProducer` loads actors born on today's date from a local CSV cache
3. If fewer than 6 actors are found, it calls `ActorBornTodayClient` to fetch the remainder from the API
4. New actors are published to `ActorTopic` via `ActorProducer` and saved to the local CSV
5. Recent news for each actor is fetched via `ActorNewsClient` and indexed by publish time (HH:mm)
6. Every second the loop checks the current time — when a scheduled minute matches, the corresponding news batch is sent to `NewsTopic`

**Static data flow (seed):**
`DataSeedRunner` orchestrates one-off API calls to populate CSV files:

| Client | Output CSV |
|---|---|
| `PopularCelebsClient` | `actor/mostPopularCelebs.csv` |
| `TopRatedMoviesClient` | `movie/topRatedMovies.csv` |
| `FilmographyClient` | `movie_actor/allFilmography.csv` |
| `ActorKnownForClient` | `movie_actor/actorKnownFor.csv` |
| `MovieMetadataClient` | `movie/knownMovies.csv` |

---

### spark-processor

Responsible for consuming Kafka events, transforming them, and persisting to Cassandra.

**ActorStreamConsumer** — consumes `ActorTopic`:
- Deserializes JSON `→` Actor fields
- Inserts into `imdb_keyspace2.actors`
- Batch interval: 20 seconds

**NewsStreamConsumer** — consumes `NewsTopic`:
- Deserializes JSON `→` News fields
- Runs `processBody()`: tokenizes article body, removes stopwords, ranks word frequency, keeps top 10 as comma-separated `common_words` (tags)
- Inserts into `imdb_keyspace2.newsTable`
- Batch interval: 20 seconds

**Tag extraction algorithm (`processBody`):**
```
article body
     │
     ▼
Lowercase + split on non-alpha characters
     │
     ▼
Remove stopwords (571 words from stopwords.txt)
     │
     ▼
Count word frequency
     │
     ▼
Sort by frequency descending
     │
     ▼
Take top 10 → stored as "common_words" in Cassandra
```

**SchemaInitializer** — one-off setup:
```
KeyspaceCreator → TableCreator → CsvDataLoader
```

---

## Data Model

### Cassandra Keyspace: `imdb_keyspace2`

#### `actors`
| Column | Type | Description |
|---|---|---|
| `idActor` (PK) | text | IMDb actor ID (e.g. `nm0000123`) |
| `name` | text | Full name |
| `birthDate` | text | ISO date string |
| `birthPlace` | text | City, country |
| `gender` | text | `male` / `female` |

#### `movies`
| Column | Type | Description |
|---|---|---|
| `idMovie` (PK) | text | IMDb title ID (e.g. `tt0000123`) |
| `rating` | text | IMDb rating (0–10) |
| `title` | text | Movie title |
| `releaseDate` | text | Release date |
| `runningTimeInMinutes` | text | Duration |

#### `topRatedMovies`
| Column | Type | Description |
|---|---|---|
| `idMovie` (PK) | text | IMDb title ID |
| `ranking` | text | Position in top 250 |

#### `actorMovies`
| Column | Type | Description |
|---|---|---|
| `idActor` (PK) | text | IMDb actor ID |
| `idMovie` (PK) | text | IMDb title ID |

#### `newsTable`
| Column | Type | Description |
|---|---|---|
| `id` (PK) | text | News article ID |
| `head` | text | Article headline |
| `body` | text | Full article body |
| `link` | text | Source URL |
| `id_actor` | text | Related actor ID |
| `publishTime` | text | ISO datetime |
| `common_words` | text | Top 10 tags (comma-separated) |

---

## Kafka Topics

| Topic | Producer | Consumer | Message format | Key |
|---|---|---|---|---|
| `ActorTopic` | `ActorProducer` | `ActorStreamConsumer` | JSON (Actor) | `idActor` |
| `NewsTopic` | `NewsProducer` | `NewsStreamConsumer` | JSON (News) | news `id` |

**Actor JSON example:**
```json
{
  "id": "nm0000123",
  "name": "Tom Hanks",
  "birthDate": "1956-07-09",
  "birthPlace": "Concord, California, USA",
  "gender": "male"
}
```

**News JSON example:**
```json
{
  "id": "rn1234567",
  "head": "Tom Hanks joins new project",
  "body": "Academy Award winner Tom Hanks...",
  "link": "https://...",
  "publishTime": "2024-04-16T14:30:00",
  "id_actor": "nm0000123"
}
```

---

## Configuration

Both modules read from `src/main/resources/config.properties`. Do **not** commit API keys — use environment variables instead.

### kafka-producer — `config.properties`

```properties
kafka.broker=localhost:9092
kafka.topic.actor=ActorTopic
kafka.topic.news=NewsTopic

# API keys — set via environment variables
# export IMDB_API_KEYS=key1,key2,key3
# export IMDB_API_KEY=key1
api.host=imdb8.p.rapidapi.com
api.retry.max=3
api.retry.sleep.seconds=2

csv.mostPopularCelebs=src/main/resources/actor/mostPopularCelebs.csv
actors.max=6
```

### spark-processor — `config.properties`

```properties
kafka.broker=localhost:9092
kafka.topic.actor=ActorTopic
kafka.topic.news=NewsTopic

cassandra.host=localhost
cassandra.port=9042
cassandra.keyspace=imdb_keyspace2

spark.batch.duration.ms=20000
spark.checkpoint.dir=/tmp/spark-checkpoint

csv.actors=src/main/resources/actor/mostPopularCelebs.csv
csv.movies=src/main/resources/movie/knownMovies.csv
csv.topRated=src/main/resources/movie/topRatedMovies.csv
csv.actorMovies=src/main/resources/movie_actor/actorKnownFor.csv
stopwords.path=src/main/resources/stopwords.txt
```

---

## Setup & Run

### Prerequisites

| Tool | Version |
|---|---|
| Java JDK | 8+ |
| Apache Kafka | 3.7.x |
| Apache Cassandra | 3.x |
| Apache Spark | 1.6.2 |
| Maven | 3.6+ |

### 1. Set your API keys

```bash
export IMDB_API_KEYS=your_key1,your_key2
```

Get API keys at [rapidapi.com](https://rapidapi.com) → search **IMDb8**.

### 2. Start infrastructure

```bash
# Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Kafka broker
bin/kafka-server-start.sh config/server.properties

# Create topics
bin/kafka-topics.sh --create --topic ActorTopic --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
bin/kafka-topics.sh --create --topic NewsTopic  --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Cassandra
cassandra -f
```

### 3. Initialize Cassandra schema

```bash
cd spark-processor
mvn compile exec:java -Dexec.mainClass="cassandra.SchemaInitializer"
```

### 4. Build

```bash
cd kafka-producer  && mvn clean package
cd ../spark-processor && mvn clean package
```

### 5. Run

```bash
# Terminal 1 — start Kafka producer (real-time streaming)
cd kafka-producer
mvn exec:java -Dexec.mainClass="ProducerApplication"

# Terminal 2 — start Spark actor consumer
cd spark-processor
mvn exec:java -Dexec.mainClass="ActorStreamConsumer"

# Terminal 3 — start Spark news consumer
cd spark-processor
mvn exec:java -Dexec.mainClass="NewsStreamConsumer"
```

### 6. (Optional) Seed static data

```bash
cd kafka-producer
mvn exec:java -Dexec.mainClass="api.DataSeedRunner"
```
Uncomment the desired steps inside `DataSeedRunner.main()` before running.

---

## Roadmap

- [ ] Upgrade Spark 1.6 → Spark 3.x (Structured Streaming)
- [ ] Add Confluent Schema Registry + Avro serialization
- [ ] Add Dead-Letter Topic for failed messages
- [ ] Add Docker Compose (Kafka + Zookeeper + Cassandra + producers + consumers)
- [ ] Add Spring Boot REST API over Cassandra
- [ ] Add Prometheus metrics + Grafana dashboards
- [ ] Add Lambda Architecture batch layer (S3/HDFS + scheduled Spark jobs)
- [ ] Add `.gitignore` for `target/` and compiled `.class` files
- [ ] Add unit tests (JUnit 4 + Mockito)
- [ ] Rename GitHub repository to `imdb-streaming-pipeline`
