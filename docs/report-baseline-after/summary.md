# After refactor

H2 fixture; actual MyBatis prepared SQL; model stub; no HTTP/AOP.

| Case | SQL count | Model calls |
|---|---:|---:|
| employee-empty-0 | 4 | 1 |
| employee-failure-0 | 4 | 1 |
| employee-offline-0 | 4 | 0 |
| employee-success-0 | 4 | 1 |
| student-empty-0 | 5 | 1 |
| student-empty-101 | 4 | 1 |
| student-failure-0 | 5 | 1 |
| student-failure-101 | 4 | 1 |
| student-failure-103 | 4 | 1 |
| student-null-0 | 5 | 1 |
| student-offline-0 | 5 | 0 |
| student-offline-101 | 4 | 0 |
| student-offline-102 | 4 | 0 |
| student-offline-103 | 4 | 0 |
| student-success-0 | 5 | 1 |
| student-success-101 | 4 | 1 |
| student-success-102 | 4 | 1 |
| student-success-103 | 4 | 1 |
