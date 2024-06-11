# dv01 take home code challenge


## Requirements
- [Java](https://adoptium.net/) (my version is 21.0.2)
- [sbt](https://www.scala-sbt.org/) (my version is 1.10.0)


## Setup

Download the `LoanStats_securev1_2017Q4.sqlite` database file (I was emailed a link) and save it to this project's root directory.


## Run

Start the server:
```sh
sbt run
```

You can now make API requests against `localhost:9000` or open the UI in a browser at http://localhost:9000


## Test

Run all tests:
```sh
sbt test
```


## REST API

After reading the `Lending Club Loan Data - Exploratory Analysis`, I tried to imagine what requests would look like for some of the sections:

- For the `summary statistics on the continuous variables` table:

    ```js
    // 1. Return selected variables with all columns as shown in the analysis
    POST /api/loans/agg_continuous
    {
      "limit": 10,
      "offset": 0,
      "selectedFields": ["loan_amnt", "funded_amnt", "dti"]
    }

    // 2. Mix and match variables and columns
    POST /api/loans/agg_continuous
    {
      "limit": 10,
      "offset": 0,
      "agg": {
        // The data set has 94 available continuous variables. Imagine the request body for a large subset, say 86 of them.
        "loan_amnt": ["min", "max"],
        "funded_amnt": ["min", "Q1", "median"],
        "dti": ["min", "Q1", "median", "mean", "Q3"],
      }
    }
    ```

- For the loan volume visualizations at the end of the analysis:

    ```js
    // "Grade A Loan Count by State"
    // AND
    // "Grade B Loan Count and Total and Mean by Date"
    POST /api/loans/agg_loan_volume
    {
      "limit": 10,
      "offset": 0,
      "agg": [
        {
          "grade": "A",
          "fields": {
            "loan_amnt": ["count"]
          }
          "groupBy": "addr_state"
        },
        {
          "grade": "B",
          "fields": {
            "loan_amnt": ["sum", "count", "mean"]
          }
          "groupBy": "date"
        },
      ]
    }
    ```

Those two examples aren't terrible but a good analyst could come up with many different stats to visualize. Are we going to create new endpoint for every one? Even if we could maintain such a large API, asking users to learn it and change their mental model is a worse experience than having them request data that they can later aggregate or slice however they want using tools they're already familiar with (e.g. Python, R, Excel if the response maps well to csv, etc...).

That's why I chose to build:
- a main endpoint that returns filtered data without aggregation
- one endpoint with aggregation for basic stats grouped by a single column

### `POST /api/loans`

<details>
<summary>Returns loans from the database with a subset of the original 148 table columns.</summary>

This request essentially maps to a `SELECT` query with support for `WHERE`, `ORDER BY`, `OFFSET`, and `LIMIT` clauses.

#### Request body:
- `limit` | nullable int, default is 10
  - The maximum number of results to return. The value is replaced by 0 if less than 0, and 100 if greater than 100.
- `offset` | nullable int, default is 0
  - The number of results to skip
- `sortField` | nullable string
  - The field to sort by
- `sortDirection` | nullable int, default is `1` (ascending)
  - The sort direction, `-1` for descending, all other values for ascending
- `filter` | nullable key-value object, default is {}
  - `state` | nullable string
  - `grade` | nullable string
  - `subGrade` | nullable string

#### Response body:
- `hasNextPage` | boolean
  - true if there are more results after the current page
- `totalCount` | int
  - the number of results before applying the filters
- `entities` | array of objects
  - the result objects have the following fields:
    - `id` | long
    - `loanAmount` | int
    - `date` | string
    - `state` | string
    - `grade` | string
    - `subGrade` | string
    - `ficoRangeLow` | int
    - `ficoRangeHigh` | int

#### Examples:

1. Get the second biggest loan

    ```sh
    curl -i localhost:9000/api/loans -X POST -H "Content-Type: application/json" -d '{
      "sortField": "loanAmount",
      "sortDirection": -1
      "limit": 1,
      "offset": 1,
    }'
    ```

2. Get the first 2 (default sort) Grade A loans in New Jersey

    ```sh
    curl -i localhost:9000/api/loans -X POST -H "Content-Type: application/json" -d'{
      "limit": 2,
      "filter": { "grade": "A", "state": "NJ" }
    }'
    ```
</details>

### `GET /api/loans/aggregate`

<details>
<summary>Returns min, max, and mean values for a continuous variable grouped by a categorical variable, e.g. loan amount by state.</summary>

#### Query parameters
- `selectField` | string
  - the continuous variable to aggregate
    - NOTE: currently supported variables are `"loanAmount"`
- `groupByField` | string
  - the cateogorical variable to group by
    - NOTE: currently supported variables are `"grade"` and `"state"`

#### Response body
- `grouping` | string
  - the current group's value, e.g. `"CA"` or `"TX"` when grouping by `"state"`
- `min` | int
  - the categorical variable's minimum value (for the group)
- `max` | int
  - the categorical variable's maximum value (for the group)
- `mean` | int
  - the categorical variable's mean value (for the group)

#### Examples

1. Aggregate loan amount by state

    ```sh
    curl -i "localhost:9000/api/loans/aggregate?selectField=loanAmount&groupByField=state"
    ```

2. Aggregate loan amount by grade

    ```sh
    curl -i "localhost:9000/api/loans/aggregate?selectField=loanAmount&groupByField=grade"
    ```
</details>

## TODO

A checklist of tasks I wanted to achieve:

- [x] README
- [x] starter project
- [x] sqlite connection
- [x] endpoint that reads from the database
  - [x] limit param
  - [x] offset param
  - [x] sort param
    - [x] add one field to the SQL query's `ORDER BY` clause
    - [ ] add multiple fields to the SQL query's `ORDER BY` clause
    - [ ] accept `"asc"` and `"desc"` instead of integers for the sort direction
  - [x] filter param
    - [x] add multiple fields to the SQL query's `WHERE` clause
    - [ ] support more fields, e.g. `date` and `loanAmount`
  - [ ] add more fields to the response
  - [ ] validate the request body to get more helpful 400 errors
- [x] simple aggregate endpoint
- [x] simple UI that calls the first endpoint
  - [ ] proper way to deal with `"play.filters.CSRF  [CSRF] Check failed because application/json for request /api/loans"`
  - [ ] add pagination to the data table
  - [ ] try a framework (e.g. React) instead of vanilla JavaScript
- [ ] break up the `Database.scala` file
- [ ] create a smaller database for the tests to make them independent of the application data
