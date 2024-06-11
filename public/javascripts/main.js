//- Initialize the data table
const grid =  new gridjs.Grid({
  columns: ["id", "loanAmount", "date", "state", "grade", "subGrade", "ficoRangeLow", "ficoRangeHigh"],
  data: [],
  sort: true,
  pagination: true,
}).render(document.getElementById("results"));

//- Form listener
document.getElementById("form").addEventListener("submit", (e) => {
  e.preventDefault();

  const state = document.getElementById("state")?.value;
  const grade = document.getElementById("grade")?.value;
  const subGrade = document.getElementById("subGrade")?.value;

  const reqFilter = {};
  if (state.length) { reqFilter.state = state; }
  if (grade.length) { reqFilter.grade = grade; }
  if (subGrade.length) { reqFilter.subGrade = subGrade; }

  const reqBody = { filter: reqFilter };

  //- TODO remove these input fields and implement real pagination
  const limit = document.getElementById("limit")?.value;
  const offset = document.getElementById("offset")?.value;
  if (limit.length) { reqBody.limit = parseInt(limit); }
  if (offset.length) { reqBody.offset = parseInt(offset); }

  fetch("http://localhost:9000/api/loans", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(reqBody),
  })
  .then(res => res.json())
  .then(data => {
    //- Reset the form
    document.getElementById("form").reset();

    //- Update the data table
    grid.updateConfig({ data: data.entities }).forceRender();
  });
});
