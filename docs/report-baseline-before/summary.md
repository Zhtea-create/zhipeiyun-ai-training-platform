# Before refactor

H2 fixture; actual MyBatis prepared SQL; model stub; no HTTP/AOP.

| Case | SQL count | Model calls |
|---|---:|---:|
| employee-failure-0 | 14 | 1 |
| employee-offline-0 | 4 | 0 |
| employee-success-0 | 14 | 1 |
| student-empty-0 | 15 | 1 |
| student-failure-0 | 15 | 1 |
| student-failure-101 | 17 | 1 |
| student-offline-0 | 5 | 0 |
| student-offline-101 | 7 | 0 |
| student-offline-103 | 7 | 0 |
| student-offline-999 | 6 | 0 |
| student-success-0 | 15 | 1 |
| student-success-101 | 17 | 1 |
