# P06-01 contract review

Revision: `f2ca12f`, initially clean tree, plus P06 tracker/contract changes.
Source inspection: `RideLog`, `VaultState`, `VaultSettings`, fuel adapter,
`FuelEfficiencyUseCase`, History, renewal journal and backup adapters.

The contract specifies rounding, categories, inclusive calendar windows,
distance coverage and full-to-full confidence before implementation. Its table
reviews partial/missed fills, no mileage, a backdated correction and renewal
double counting. Existing service integer totals and linked renewal fee logs
remain authoritative. Budgets remain separate from paid expenses. Optional
fields preserve old JSON defaults without a competing durable writer.

This is a source/specification review. Implementation and native evidence are
recorded by the subsequent P06 tasks and the consolidated phase verification.
