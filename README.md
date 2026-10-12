# Team B — Placement Ranking Engine

A Spring Boot 3.2 microservice that fetches eligible placement applications
from **Team C's backend**, ranks candidates using three implemented algorithms,
and exposes a REST API for the ranked results.

---

## Quick Links

| Item | Value |
|------|-------|
| Team C Backend | `https://rit-placement-team-c-backend.onrender.com` |
| Java | 17 |
| Spring Boot | 3.2.3 |
| Build tool | Maven |
| Default port | 8080 (overridden by `PORT` env var on Render) |

---

## Local Development

### Prerequisites

- JDK 17+
- Maven 3.8+ (or use `./mvnw`)

### Run

```bash
# Clone / open the project root (where pom.xml is)
cd team-b-ranking-engine

# Run without building a fat JAR
mvn spring-boot:run

# Or package first then run
mvn package -DskipTests
java -jar target/team-b-ranking-engine-1.0.0.jar
```

The server starts at **http://localhost:8080**.

### Run Tests

```bash
mvn test
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `8080` | Server port (Render sets this automatically) |
| `TEAM_C_BASE_URL` | `https://rit-placement-team-c-backend.onrender.com` | Team C base URL |
| `TEAM_C_TIMEOUT_SECONDS` | `30` | HTTP timeout for Team C calls |
| `TEAM_B_RANKING_WRITE_ENABLED` | `false` | Enable ranking write-back to Team C |
| `TEAM_B_RANKING_WRITE_URL` | `https://...onrender.com/api/v1/drive-rankings` | Write-back URL |

Copy `.env.example` to `.env` for local overrides.

---

## API Endpoints

### `GET /health`
Liveness / readiness probe used by Render.

**Response 200:**
```json
{
  "status": "UP",
  "service": "team-b-ranking-engine",
  "version": "1.0.0",
  "timestamp": "2026-10-11T17:00:00Z"
}
```

---

### `GET /api/v1/rankings/eligible/{driveId}`
Fetches all eligible candidates for a drive from Team C (no ranking performed).

**Example:** `GET /api/v1/rankings/eligible/DRV001`

**Response 200:**
```json
{
  "driveId": "DRV001",
  "candidateCount": 5,
  "candidates": [
    { "studentId": "STU001", "cgpa": 9.28, "backlogs": 0,
      "aptitudeScore": 0.0, "skills": ["Java","Python","MongoDB","Node.js","React"] }
  ]
}
```

---

### `POST /api/v1/rankings/compute`
Ranks a manually-supplied candidate pool (useful for testing).

**Request:**
```json
{
  "driveId": "DRV001",
  "candidatePool": [
    { "studentId": "STU001", "cgpa": 9.28, "backlogs": 0,
      "aptitudeScore": 0.0, "skills": ["Java","Python"] }
  ],
  "algorithm": "MERGE_SORT",
  "graphVersion": "v1",
  "optimizationMetric": "SCORE_DESCENDING",
  "constraints": { "topK": 10 }
}
```

**Supported algorithms:** `MERGE_SORT` (default), `HEAP_TOPK`, `WEIGHTED`

**Response 200:**
```json
{
  "rankingId": "RANKING_READY",
  "algorithm": "MERGE_SORT",
  "exploredCandidateCount": 1,
  "orderedCandidates": [
    { "studentId": "STU001", "totalScore": 11.28, "rank": 1 }
  ]
}
```

---

### `POST /api/v1/rankings/rank/{driveId}`
**Full pipeline:** fetches eligible candidates from Team C → ranks them → (optionally) sends results back.

**Example:** `POST /api/v1/rankings/rank/DRV001`

**Optional body:**
```json
{ "algorithm": "MERGE_SORT", "constraints": { "topK": 5 } }
```

**Response 200:**
```json
{
  "driveId": "DRV001",
  "writeBackSent": false,
  "writeBackNote": "Write-back disabled — Team C does not yet implement POST /api/v1/drive-rankings.",
  "ranking": {
    "rankingId": "RANKING_READY",
    "algorithm": "MERGE_SORT",
    "orderedCandidates": [...]
  }
}
```

---

## Ranking Algorithms

| Algorithm | Class | Description |
|-----------|-------|-------------|
| `MERGE_SORT` | `MergeSortRanking` | Sorts by `cgpa + aptitude/10 + skillCount` using merge sort |
| `HEAP_TOPK` | `HeapTopKRanking` | Uses a min-heap to extract top-K candidates efficiently |
| `WEIGHTED` | `WeightedScoreRanking` | Weighted sum of normalised CGPA, aptitude, and skill count; weights supplied in request `constraints.weights` |

---

## Team C Integration

### Endpoints used

| Method | URL | Purpose |
|--------|-----|---------|
| GET | `/api/v1/applications` | Fetch all applications; filter by `driveId` and `eligibility.result == ELIGIBLE` |
| GET | `/api/v1/students` | Fetch student profiles for enrichment |
| GET | `/api/v1/drives` | (Available, used for drive context) |

### Write-back endpoint (NOT YET IMPLEMENTED by Team C)

Team B is ready to POST ranking results once Team C implements:

```
POST /api/v1/drive-rankings
```

**Proposed request body:**
```json
{
  "driveId": "DRV001",
  "rankingId": "RANKING_READY",
  "algorithm": "MERGE_SORT",
  "graphVersion": "TEAM-CURRENT",
  "tieBreakRule": "Higher score first, student ID as tie-break",
  "rankedCandidates": [
    { "rank": 1, "studentId": "STU001", "totalScore": 11.28 }
  ]
}
```

**Idempotency:** Team C should upsert using `driveId` as the natural key
(`findOneAndReplace` with `{ driveId }` filter in MongoDB) to prevent
duplicate records on repeated calls.

Enable write-back by setting `TEAM_B_RANKING_WRITE_ENABLED=true` in the
Render environment once Team C confirms the endpoint is live.

---

## Render Deployment

### Build command
```
mvn clean package -DskipTests
```

### Start command
```
java -jar target/team-b-ranking-engine-1.0.0.jar
```

### Environment variables (set in Render dashboard)

| Variable | Value |
|----------|-------|
| `PORT` | Set automatically by Render |
| `TEAM_C_BASE_URL` | `https://rit-placement-team-c-backend.onrender.com` |
| `TEAM_B_RANKING_WRITE_ENABLED` | `false` (until Team C is ready) |

### Health check path
```
/health
```

---

## Project Structure

```
team-b-ranking-engine/
├── pom.xml                              # Maven build descriptor (project root)
├── .gitignore
├── .env.example
├── README.md
└── src/
    ├── main/
    │   ├── java/com/placement/teamb/
    │   │   ├── TeamBApplication.java    # Spring Boot entry point
    │   │   ├── controller/
    │   │   │   └── RankingController.java
    │   │   ├── dto/
    │   │   │   ├── EligibleCandidate.java
    │   │   │   ├── RankingRequest.java
    │   │   │   └── RankingResult.java
    │   │   ├── integration/
    │   │   │   ├── TeamCClient.java           # HTTP client
    │   │   │   ├── TeamCException.java
    │   │   │   ├── TeamCApplicationsResponse.java
    │   │   │   ├── TeamCStudentsResponse.java
    │   │   │   ├── TeamCDrivesResponse.java
    │   │   │   ├── EligibleApplicationService.java
    │   │   │   ├── TeamBCandidateService.java
    │   │   │   ├── TeamCApplicationClient.java
    │   │   │   ├── TeamCStudentService.java
    │   │   │   └── RankingWriteBackService.java
    │   │   ├── model/
    │   │   │   ├── CandidateScore.java
    │   │   │   ├── DriveProfile.java
    │   │   │   ├── DriveRequirement.java
    │   │   │   └── RankingCandidate.java
    │   │   ├── ranking/
    │   │   │   ├── RankingService.java        # interface
    │   │   │   ├── RankingDispatcher.java     # algorithm router
    │   │   │   ├── MergeSortRanking.java
    │   │   │   ├── HeapTopKRanking.java
    │   │   │   ├── WeightedScoreRanking.java
    │   │   │   └── WeightedScoreCalculator.java
    │   │   ├── matching/
    │   │   │   └── SkillMatcher.java
    │   │   └── graph/
    │   │       ├── SkillGraph.java
    │   │       ├── AdjacencyListGraph.java
    │   │       └── AdjacencyMatrixGraph.java
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/placement/teamb/
            ├── TeamBApplicationTest.java
            └── ranking/
                └── RankingAlgorithmTest.java
```
