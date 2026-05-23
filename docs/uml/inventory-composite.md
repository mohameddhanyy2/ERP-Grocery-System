# Inventory Module — Composite Structure UML (Member 2)

This diagram shows `CentralInventoryBean` as a **composite component** that
contains three `StoreInventoryBean` **leaf components**. It demonstrates two
learned concepts at once: **Composite Structures** and **Inversion of Control**.

## Composite structure diagram

```text
                  ○ ITotalStock        ○ IStockAlerts
                  │                    │
                  │  (provided by the composite — chain-wide operations)
        ┌─────────┴────────────────────┴───────────────────────────┐
        │  «composite component»                                   │
        │  CentralInventoryBean                                    │
        │                                                          │
        │   ┌──────────────────┐  ┌──────────────────┐  ┌────────┐ │
        │   │ store_a :        │  │ store_b :        │  │store_c │ │
        │   │ StoreInventoryBean│ │ StoreInventoryBean│ │  : ... │ │
        │   └────────┬─────────┘  └────────┬─────────┘  └───┬────┘ │
        │            ○                     ○                ○      │
        │      IStoreInventory       IStoreInventory   IStoreInv.  │
        │            ▲                     ▲                ▲      │
        │            └─────────┬───────────┴────────────────┘      │
        │      internal assembly: the composite delegates to       │
        │      each leaf through its provided IStoreInventory      │
        └──────────────────────────────────────────────────────────┘

   ○ = provided interface (lollipop)
```

## How to read it

| Element | Meaning |
| --- | --- |
| `CentralInventoryBean` | The outer **composite component**. |
| `store_a/b/c : StoreInventoryBean` | Three **leaf parts** held inside the composite. |
| `IStoreInventory` lollipops (inner) | Each leaf **provides** the per-store contract: `checkStock`, `updateStock`, `getLowStockAlerts`, `getStoreId`. |
| `ITotalStock` lollipop (outer) | The composite **provides** chain-wide stock: `getTotalStock`, `getStoresWithLowStock`, `redistributeStock`. |
| `IStockAlerts` lollipop (outer) | The composite **provides** restock checks: `isRestockNeeded`, `getProductsNeedingRestock`. |
| Internal assembly | The composite implements its outer interfaces by **looping over the leaves** through their `IStoreInventory` interface. |

## Concepts demonstrated

- **Composite Structure** — `CentralInventoryBean` holds a
  `List<IStoreInventory>` and treats one store and the whole chain through
  the same interface type. A caller cannot tell the difference.
- **Inversion of Control** — leaf stores are **injected** from outside via
  `addStore(IStoreInventory)`. The composite never calls `new StoreInventoryBean()`.
- **Provided / Required interfaces** — the composite **provides**
  `ITotalStock` and `IStockAlerts`; it **requires nothing** (foundation
  component).

> The `.puml` source is in `inventory-composite.puml`. Render it with any
> PlantUML tool to get the formal diagram.
