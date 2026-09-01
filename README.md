> [!IMPORTANT]
> **This is an archival branch. It is frozen and will not receive updates.**
> For maintained code, releases and issues, go to [`main`](https://github.com/palexdev/VirtualizedFX/tree/main).

## About this branch

`v1-table-wip` marks the last commit at which the original `VFXTable` implementation still exists.
Everything past this point on `main` replaces it with a rewrite done from scratch.

The branch shares its whole history with `main`, nothing is exclusive to it. It exists to give that point a name.

This is unreleased work of a table with a couple extra features (x-axis virtualization and blank space fill policy). It is feature-complete but not stable, there are known bugs.

### Why it was rewritten

Fixing those bugs and fitting in the features kept running into the same wall: the
architecture was hard to manage and to evolve.

Most of the bugs were silent, and caused by registration order of the listeners ultimately responsible for updating the table's state.
In fact, some of them were swapped from InvalidationListeners to ChangeListeners, because they fire in a different order in JavaFX, and this would allow us to override the priority to some degree.
But, that was not enough in some cases and it wasn't even a clean thing to do.

The rewrite is about making that complexity declared and localized rather than emergent. For example, instead of letting listeners do their own thing in order (which could get scrambled in some occasions), having latches or flags to prevent "re-fire" and potential StackOverflowErrors, in v2 we manage the order of operations in a single listener. That is the new general rule.
