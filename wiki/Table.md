[//]: @formatter:off

# VFXTable

## Specs

- Style Class: `vfx-table`
- Default Skin: `VFXTableSkin`
- Default Behavior: `VFXTableManager`
- Default columns:
- Default rows:

### Properties

| Property                 | Description                                                                            | Type           |
|--------------------------|----------------------------------------------------------------------------------------|----------------|
| 1) items                 | Specifies the list containing the items to display                                     | ObservableList |
| size(delegate)           | Specifies the size of the list                                                         | Integer        |
| empty(delegate)          | Specifies whether the list is empty                                                    | Boolean        |
| 2) columns               | The list containing the table's columns                                                | ObservableList |
| 3) rowFactory            | Specifies the function used to build the rows                                          | Function       |
| helper                   | Specifies the VFXTableHelper instance, a utility class which defines core computations | VFXTableHelper |
| helperFactory            | Specifies the function used to build a VFXTableHelper instance                         | Function       |
| 4) vPos                  | Specifies the viewport's vertical position                                             | Double         |
| 4) hPos                  | Specifies the viewport's horizontal position                                           | Double         |
| 5) virtualMaxX(delegate) | Specifies the total number of pixels alongside the x-axis                              | Double         |
| 5) virtualMaxY(delegate) | Specifies the total number of pixels alongside the y-axis                              | Double         |
| state                    | Specifies the state object, which represents the current table's state                 | VFXTableState  |
| needsViewportLayout      | Specifies whether the viewport's layout needs to be computed                           | Boolean        |

### Styleable Properties

| Property             | Description                                                                               | CSS Property              | Type                    | Default Value |
|----------------------|-------------------------------------------------------------------------------------------|---------------------------|-------------------------|---------------|
| 6) rowHeight         | Specifies the height of each row (cells implicitly)                                       | -vfx-row-height           | Double                  | 32.0          |
| 7) columnSize        | Specifies the width and height of each column                                             | -vfx-column-size          | Size                    | [100.0, 32.0] |
| 8) columnsLayoutMode | Specifies whether the columns should have fixed sizes                                     | -vfx-columns-layout-mode  | Enum(ColumnsLayoutMode) | FIXED         |
| 9) bufferSize        | Specifies the extra number of rows and columns to render (makes scrolling smoother)       | -vfx-buffer-size          | Enum(BufferSize)        | Standard(2)   |
| clipBorderRadius     | Specifies the radius of the clip applied to the grid (to avoid rows and columns overflow) | -vfx-clip-border-radius   | Double                  | 0.0           |
| cellsCacheCapacity   | Specifies the maximum number of cells to cache for each column (local to columns)         | -vfx-cells-cache-capacity | Integer                 | 10            |
| rowsCacheCapacity    | Specifies the maximum number of rows to cache (in table)                                  | -vfx-rows-cache-capacity  | Integer                 | 10            |


### Additional capabilities

- Auto-size columns (VARIABLE mode only)

## Internals

_**Architecture**_
- Table (Model): defines the capabilities/features
- Table Skin (View): defines the view, mostly handles layout. This is different compared to other components as the
  `viewport` node is not enough. We need at least two nodes to arrange the columns and the rows.
- Table Manager (Behavior/Controller/Manager): reacts to inputs and properties changes to produce new states
- Table Row: responsible for containing and laying out cells, as well as update them as needed
- Table Column: header and separator for the cells; responsible for creating the cells

<br >

**Proof of concept**  
The table is much more complex compared to the list and the grid. Trivially, we could say that it is just a mix between
the two, because we have a 1D structure where each item is a **row**, but then in every row there is a cell for each column (2D layout).  
However, practically speaking, things are not so simple. Although **rows** and **columns** are not new concepts,
in the table's context they become UI nodes.  
In theory, a layout similar to the Grid's one could be implemented, but it actually would make some things much more complex to implement.
Some examples:
- Columns act as headers and separators for each type of cells, they can have fixed or variable width.
- Rows are containers for the cells and allow to easily implement features such as row highlighting/selection.
- Vertical scrolling is different here, because in such case we want to "move" the rows but **not** the columns.

As a consequence of rows and columns being UI nodes in the table, they should **not be interfaces** but rather
**abstract classes**.

### Columns

The base class for the columns, `VFXTableColumn`, should extend `Labeled` and implement at least these core functionalities:
- A **factory** to produce cells. Each column should be responsible for producing cells for a certain type of data,
  let's see a practical example:
  ```java
  // Our data model, a City
  public record City(String name, int postalCode, int population) {}

  // The first column will show a "Name" label as the header, and produce cells that do the following mapping city -> city.name()
  // The second column will show a "Postal Code" label as the header, and produce cells that do the following mapping city -> city.postalCode()
  // The third column will show a "Population" label as the header, and produce cells that do the following mapping city -> city.population()
  ```  
  **Alternatively**, we could move the cell factory to the table, thus having **only one**, and base the data
  mapping on the cell index. This solution however, may result in a **less versatile** and **less user-friendly** component.  
  I should **investigate** on this  matter as this could change some core parts of the architecture.
- A **cache** to store cells that are not needed anymore. The cells cache is not one and is "local" to each column, this
  is a consequence of the above property (**changes** in case of alternative scenario).
- The **table** instance. Each column should store the instance to the table they are assigned to.
  For ease of use, the table should handle this property **automatically**, meaning that when columns are added/removed
  from the table, this property should be set accordingly.

### Rows

As for the rows' base class, `VFXTableRow`, it's enough to extend `Region` as it is a mere container for cells, a custom skin
would be redundant.  
However, from a "logical" point of view, rows are a bit more complex than that. In fact, since they manage the cells
they have to be stateful components, which means they should store information such as:
- The **columns range** which implicitly will give us the required cells to display
- The **index** to allow knowing in which row they are, as well as what item from the list is being displayed
- The **item** which is important to the cells to achieve the aforementioned mapping (see the above code example)
- The **cells** contained in itself. This in particular may be problematic depending on how we store them.
  The simplest solution would be to use a **map** of type **[Integer -> Cell]**, but further below I'm going to discuss
  the pros and cons, as well as an alternative way.  
  The **layout is unaffected** since it is **absolute** (so the actual column index is irrelevant), but it's crucial to
  answer the following questions:
   - What happens when columns are **added/removed**?
   - What happens when a **permutation** happens (columns switching places)?
- The **table** instance. Each row should store the instance to the table they are assigned to.
  For ease of use, the table should handle this property **automatically**, meaning that when rows are created/disposed,
  this property should be set accordingly.

<br >

#### _On changes/events/actions_
_Note 1: Numbers above and below are correlated ofc_  
_Note 2: Every **change** should produce and set a **new state**_  
_Note 3: The following "explanations" should not be considered the real implementation as in reality things may differ.
These are more like dev notes that helped me build the system by figuring out the core concepts/mechanisms._  
_Note 4: changes starting with '[ R ]' are rows-related; changes starting with '[ C ]' are columns related; changes starting
with [ T ] are table-related meaning that they could affect the whole system or partially. "Related" may also simply mean:
"the computation/method will be in the relative class"._

0) **[ T ]** Init depends on geometry (width/height). It's simple as it's enough to ensure that the viewport has the right amount of rows and cells.
   Old rows and cells do not need to update, just disposal or caching (in case there are too many)
   It's also important to ensure the positions are valid!
1) **[ R ]** Very similar/same algorithm used for the list and the grid. The only difference here is that instead of working on the
   cells directly, we work on the rows. Rows in common are moved to the new state, removed/unnecessary rows are reused/disposed.
   When a row is updated, in particular its **item** property, it has to update its cells too by doing something like this:
   ```java
   cells.forEach(c -> c.updateItem(newItem));
   ```
2) **[ R ]** This is complex; basically, there are two approaches I can think of:
    1) The first approach is simple but less efficient. When a change occurs, just reset the rows' state by clearing and
       re-computing the cells' map. Since columns use a cache to store the cells, in theory the performance difference should
       be negligible; although it's worth noting that if such changes occur frequently, then this difference **may** definitely be
       impactful.
    2) The second approach is to use the `StateMap` to store the cells thus having a double mapping like this:  
       `[Integer -> Cell]` and `[Column -> Integer]` which resolves to `[Column -> Cell]`.    
       The below example shows in pseudocode how the algorithm could work doing it this way.  
       <br >
       <details>
       <summary><i>PseudoCode Example</i></summary>
       
       ```java
       /*  
        * Examples of changes
        * Range:              [0, 3]
        * Starting list:      [0:A, 1:B, 2:C, 3:D]
        *       
        * Switch/Permutation: [0:C, 1:B, 2:A, 3:D]
        * Add at 0:           [0:Z, 1:A, 2:B, 3:C, 4:D]
        * Remove at middle:   [0:A, 1:C, 2:D]
        */
       ColumnsMap map = ...;
       IntegerRange range = ...;
       Set<Integer> expanded = IntegerRange.expandRangeToSet(range);
       
       for (Integer index : range) {
           TableColumn column = table.getColumns().get(index);
           Cell c = cells.remove(column); // !!
       
           // Commons
           if (c != null) {
               expanded.remove(index);
               c.updateIndex(index);
               map.put(index, column, c); // !!
               continue;
           }
       
           // New columns
           map.put(index, column, getCell(index));
       }
       
       dispose();
       this.cells = map;
       this.columnsRange = range;
       onCellsChanged(); // To update children
       // Make sure to also call requestViewportLayout() where appropriate to update the layout
       ```
       </details>
       <br >
       It is very similar to the one used to manage items changes (for all containers: list/grid/table). Extracting common  
       columns is straightforward and efficient. When the cell is not found (null), it means that the column is "new" to the  
       range. Of course, those who are not outside the range cannot be reused, because as already mentioned before each column  
       build a "type" of cell.
       <br >
       
       <br >
       <details>
       <summary><strong>Important notes</strong></summary>
       <ul>
       <li>
          The <code>StateMap</code> is never reused, rather, every new state has a new map which is built as needed. To facilitate things,
          the same should apply here.<br>
          The <strong>current</strong> map is going to be used to extract common columns and dispose the remaining entries.<br>
          A <strong>new</strong> map should be declared before starting the computation, and only after the disposal it should take
          the place of the old one.<br>
          This is also needed because the <code>StateMap</code> implementation does not allow it.
       </li>
       <li>
          This way, at the end, it's enough to update the children.<br>
          <strong>However</strong>, to ensure the cells are correctly laid out, a call to <code>requestViewportLayout()</code> must be done.<br>
          I believe the best place for this is to do it at the end of the method in the <strong>table manager</strong>.<br>
          (<strong>Cannot</strong> be done in the row as this would trigger the method multiple times)
       </li>
       </ul>
       </details>
       <br >

    In **both cases**, there's also another thing to consider. Remaining cells need to be disposed/cached.  
    If a column is removed from the table, I believe caching its cells should be fine.    
    In case the column is going to be **reused** in the same table or in another one in the future, then there would
    be no need to build the cells.  
    In case the column is **not going to be reused** then we should make sure that the GC can do its job.  
    Which means that both the column and the cells should not hold any reference to any other component of
    the system (item and table set to null should be enough).  
    <br >
    It's **important** to consider that changes in the columns list may lead to a different range.  
    This means that in the table manager's method a **new state** should be computed **if and only if** the range changes.  
    In any case, it's clear that the columns range must be computed everytime and passed to the method of each row.  
    (see how in the above pseudocode, the row needs the range for the computation)  
    <br >
    There's also an **edge** case to handle here. Since the columns store the instance to the table they are assigned to,
    the table manager's method is also responsible for setting it for columns that are added/removed.  
    At init time, since the listener is added in the skin, the method needs to be called with a `null` parameter, in such
    occasion, there's no need to update the rows of course.
3) **[ T ]** ~~Same algorithm used for the other containers when the cell factory changed.  
   Current rows are to be **disposed**, which means that all the **cells** are going to be **cached** by the "parent" column.  
   When new rows are created the old cells will be reused. No need to recompute positions and sizes.~~  
   Let's not forget we are dealing with a bi-state component, there's the **viewport state** and then the **rows' state**
   (each row has its own state). The first is used to keep track of which and how many rows there are in the viewport.
   The second is used by each row to know which and how many items/cells they should contain.  
   A consequence of such architecture, in this specific case, is that the viewport state becomes invalid, but the state of
   each row technically remains valid. Which means that there is no need to cache the cells and thus wasting performance,
   it's enough to move them over to the new respective row. Let's see a pseudo-code example:  
   ```java
   // Row factory changes
   // As usual we delegate the update to the manager...
   VFXTableState<T> state = ...; // Current state
   // Make sure we can create a valid new state before the actual computation
   
   // Ranges do not change, as well as positions
   // Be careful though, if the old state is EMPTY then we can't take the ranges from it, but rather we need to ask the VFXTableHelper
   IntegerRange rowsRange = (state != VFXTableState.EMPTY) ? state.getRowsRange() : helper.rowsRange();
   IntegerRange columnsRange = (state != VFXTableState.EMPTY) ? state.getColumnsRange() : helper.columnsRange();
   VFXTableState<T> newState = new VFXTableState<>(...);
   
   // Iterate over the rows range and create the new rows using the new factory
   Function<T, VFXTableRow<T>> newFn = ...;
   for (Integer idx : rowsRange) {
       T item = ...; // get item at index idx
       VFXTableRow<T> oldRow = state.getRows().get(idx); // Get the old row at the same index
       VFXTableRow<T> row = newFn.apply(item);
       // Here's where magic happens
       row.copyState(oldRow);
       // As the name suggests, we are telling the new row to copy the state of the old row, what this means is:
       // 1) The new row will have the same index of the old one
       // 2) The new row will have the same columns range of the old one
       // 3) The new row will use the cells map of the old one, and the latter will have its cells map instance set to null (important for disposal, we don't want to dispose the cells too!)
       // 4) Each cell in the map has to be updated by invoking cell.updateRow(this) so that every cell can store the correct row instance
       // 5) Add the cells to the row by invoking onCellsChanged()
       
       // Beware!!
       // The above code is a little different in reality. If the old state is EMPTY then we can't get any old row to copy the state from
       // Which means that the new row will need a new state by invoking row.updateIndex(idx) and row.updateColumns(...)
   
       // Finally we can add 'row' to the new state
       newState.addRow(idx, item, row);
   }
   
   // The above code should execute only if preliminary checks have passed
   // The below code will run always
   state.dispose(); // Dispose the old state, the old rows are not needed anymore. Beware, this will !!clear!! and cache the rows
   table.getCache().clear(); // This will complete the disposal of the old rows by removing them from the cache and also invoking the dispose() method on each of them
   
   // The below code runs only if a new state was generated newState != null
   newState.setRowsChanged(true); // This will trigger a layout call
   table.update(newState);
   ```
4) **[ R ]** When the **vertical** position changes, I believe the same algorithm used for the other containers can be used,
   it's enough to update the rows with the items that are not in the viewport anymore (which implicitly means updating the cells).  
   When the **horizontal** position changes then we need to iterate over each row and update their columns range.
   Unfortunately, the computation in this case is a little heavier on performance because, remember, **cells cannot be reused**
   as each column produces its type of cells.
5) **[ T ]** In theory, these changes only when other kinds of changes occur.
6) **[ T ]** Ensure correct number of cells; Update 5; Ensure valid position; Layout
7) **[ T ]** Independently of the columns layout mode, it should be enough to compute both the columns and rows ranges
   and check if they differ from the old ones.  
   If the **columns range** changes, then it's enough to iterate over the rows and give them the new range
   (internally they will update themselves ofc).  
   If the **rows range** changes, it means that the **height** changed, so it may happen to have more or less rows than needed.
   Simply add more or dispose unneeded ones.  
   In other words, I believe this could be treated as a geometry change.
8) **[ T ]** This may seem a complex one, but in reality it's simple, we just need to differentiate between two cases:
    1) **FIXED to VARIABLE:** positions are valid, rows range not going to change. The only things that changes is the
       columns range because in **VARIABLE** mode, virtualization alongside the x-axis is disabled, which means that all
       columns will be added to the viewport. Consequently, all their corresponding cells will be created and added to the container.  
    <br >
    2) **VARIABLE to FIXED:** the vertical position is valid (since only the columns' width may have changed), and so is
       the rows range too. Update 5; Invalidate positions (only hPos may change); Compute the new columns range and update the rows.
9) Virtualized containers should have buffer cells both at the top and bottom of the viewport for a smoother scroll experience.
   When the number of buffer cells changes, it's enough to act as if the geometry changed since only one thing can happen:
   more or less cells than needed.

### When cell factories change...

As already mentioned many times before, each column in the table is responsible for building its own "type" of cells, and
this is indeed a major difference when compared to other containers.  
Because of this difference, process such cases is not as straightforward. Although it happens in the columns, the work
has to be delegated to the rows since they are the ones responsible for containing and mannaging the cells.  
So, I can think of two ways to handle this (also adding a bit of history):
0) In _ancient times_ awful tricks and workarounds were used. A listener in the column's skin detected the change and
   called a _delegate_ method in the table. The actual method was in the _manager_, which then delegated the update to
   the current state. At this point, the state class would iterate over the rows and call the update method.  
   However, before doing so, the column's index was retrieved. To avoid performance issues, instead of using `indexOf(...)`
   a `Map` was built in the table which kept the columns mapped by their position in the list.  
   A fine workaround indeed, but you can clearly see how messed up all of this is, summed it up in symbols, here's the flow:  
   `[Column Skin]->[Table]->[Manager]->[State]->[Column to index]->[Rows]`
1) The current new implementation uses an **event bus** to carry the change directly from the column to the rows.  
   When the factory changes, the column publishes an event carrying itself, the old and new factories.
   By **subscribing** to such events, the rows can proceed with the update.  
   <br >
   The **good** thing is clearly having a shorter and much more clean flow:  
   `[Column]->[Event]->[Rows]`  
   <br >
   A possible **issue** could be the use of a middleman (the event bus) because we are "bypassing" the table manager. In other words, we are
   **not creating a new state**, which in theory it's fine since the ranges do not change, but it comes with a
   dilemma, discussed [here](#_to-generate-or-not-generate-a-new-state_).  
   It's also true that we could **subscribe to the event in the manager**, but still I prefer the solution below, as it
   requires less code, less memory and it's more performant.
2) The **preferred** way would be to just use the table manager from the column as follows:
   ```java
   VFXTable table = getTable();
   VFXTableManager manager = table.getBehavior();
   manager.onCellFactoryChanged(column, oldFactory, newFactory);
   ```
   The flow becomes:  
   `[Column]->[Manager]->[Rows]`  
   _(Not counting the table as we are just getting the manager instance)_
   <br >  
   Pros:
      - No need for an event bus, which doesn't harm performance in theory, but still...
      - Each class does what it is responsible for, so we are honoring the SRP, good encapsulation.

   <br >
   
There is, however, a **common issue** shared by all implementations.  
In all cases, we have the column as one of the inputs, but that is not enough to know which cell in each row is to be replaced.
So, we still would need to get its index by using `indexOf(...)` (**NOT feasible**, too heavy on performance).  
**However**, this is only true in case cells are mapped as `[Integer -> Cell]`.  
As discussed [above, at point 2](#_on-changeseventsactions_); in case we use a `StateMap`, we could retrieve and
manipulate the cells by using the **column as a key**.

### The last column

In table components, there is a common convention about the last column: _if the container is bigger than all the
columns' width combined, then the last column should take up all the remaining empty space_.  
This is quite problematic for the architecture of VFXTable. In other words, the last column is breaking one of the core
rules of virtualization: all cells/subcomponents must have fixed size, so that values such as the max scroll, estimated/virtual
length,... can be computed quickly and without fail.  
Not only that, since the virtualized containers react to geometry changes (width/height changes), in terms of layout,
only and only if the range of items to display also changes. This assumption is also broken by the aforementioned convention,
because every time the table's width changes we must ensure not only that the last column is resized properly, but also the
rows and their cells. All of this simply means: _potential performance issues_.  
There are indeed ways to mitigate it, let's analyze the situation by checking what needs to be updated and how:
  
**Issues**
1) **virtualMaxX:** we may assume the estimated width is simply `columnsNum * columnsWidth` but doing so will lead to an
   incorrect value in case the table's width is bigger. If we have 5 columns of 100px width each, and the table is 500px wide,
   the estimated width will be **600px** not 500px because the last column must be resized to 200px.
2) **layoutColumns:** to ensure the last columns is sized correctly, every time the table's width changes we must invoke
   the `layoutColumns()` method. However, doing so is a waste, because technically we just have to resize the last column.
3) **layoutRows:** as for the rows, there is no other way than resize all the rows in the viewport by calling `layoutRows()`.
4) **layoutCells:** the `layoutRows()` invocation will also lead to the layout of all the cells, and this is also a waste
   because the only cells to resize are the last column ones. Hence, we need to perform a partial layout.

**Solutions**
1) We could modify the computation as follows `Math.max(tableWidth, columnsNum * columnsWidth)`. In **FIXED** layout mode,
   there is only one scenario for the last column to be bigger than the fixed size: when the table is bigger than `columnsNum * columnsWidth`.
   In other words, if the table's width is greater than the estimated width, than that's our `virtualMaxX`.  
  
_**Note:** before invoking the layout methods, we must determine where and how to do it. There are two places that can catch
such changes: the manager's `onGeometryChanged()` and the skin's listener. The issue about the first one is that it also
triggers for height changes, so we may end up calling the layout methods when unnecessary. On the other hand the second
place does not have such issue, but does not allow for easy customization on the user-end.  
**Ideally we should offer an in-between solution**._

2) We could modify the method to accept an index parameter, which basically means we want to lay out the columns starting
   from the given index to the end given by the current columns range. If the index is -1 or outside the range, simply
   perform a full layout. This is also good to support partial layout for the **VARIABLE** layout mode.  
   Unfortunately, we can't retrieve the index of a column without relying on the too slow `indexOf()` method, so instead
   we have to pass the starting column as parameter, figure out it's index in a loop, and then use the index on the cells too.
3) The only change we could make to the rows would be to bind their width to the table's width. However, it is not very
   useful. Since we need to identify such changes, and consequently trigger the layout methods (when needed), there is no
   benefit in automatizing it for the rows; after all, their layout method will also handle the cells.  
   For this reason, we should modify this method too to accept an index parameter which is going to tell us which cells
   need to be laid out, in other words, it would allow for a partial layout here too.
4) Same 3, basically pass the index parameter here too and perform a partial layout when a full one is not needed.

### Variable layout mode

Handling layout while in **VARIABLE** mode is definitely a complex situation. There are two ways I can think of:
1) The **old implementation** did not use any special flag to check whether a layout was needed, but rather it was relying
   on the JavaFX system. It was fine and performant anyway, as positions and sizes were cached and invalidated only in certain
   occasions. So, it is indeed a feasible solution and while it's relatively simple to implement, it still requires a lot
   of code and attention.
2) A **new solution** could be to use listeners. By adding a listener to its width property, the column can than inform
   the table manager of the change and then ask the rows to update the layout. The **good** thing here is that we can
   perform a **partial layout**.  
   Let's suppose we have a columns range of `[1, 7]`, and that column `5`'s width changed.
   We can start processing the layout directly from index `5` to `7`.  
   Flow of execution: `[Column]->[Manager]->[Rows]`  
   <br >
   <details>
   <summary><strong>Possible pseudocode implementation</strong></summary>
   
   ```java
   // Column class
   {
       // Using MFXCore utility class because it's so cool!
       VFXTable table = getTable();
       When.onChanged(widthProperty)
           .condition(() -> table != null && table.getColumnsLayoutMode() == VARIABLE)
           .then((ow, nw) -> table.getBehavior().onColumnWidthChanged(this, ow, nw))
           .listen(); // This needs to be disposed eventually
   }
   
   // Manager class
   {
       VFXTable table = getNode();
       VFXTableHelper helper = table.getHelper();
       VFXTableState current = table.getState();
       helper.invalidatePos(); // Important!
       current.getRows().values().forEach(r -> r.onColumnWidthChanged(column, oldWidth, newWidth));
       // Generate and set new state?
   }
   
   // Row class
   {
       VFXTableHelper helper = getTable().getHelper();
       int startIndex = cells.get(column); // From the StateMap, resolving mapping of type [Column -> Integer]
       for (int i = startIndex; i <= columnsRange.getMax(); i++) {
           Node node = cells.get(startIndex).toNode();
           helper.partialLayout(startIndex, i, oldWidth, newWidth, node);
       }
   }
   
   // Helper class
   {
       // The column that changed only needs to be resized, the position is the same
       if (startIndex == i) {
           node.resize(newWidth, node.getLayoutBounds().getHeight());
           return;
       }
   
       // Other columns have the same size as before, but need to be shifted on the x-axis
       // In theory, the shift value is the difference between the new and old width
       // IMPORTANT: we may want to snap the x position to avoid strange values (needs experimentation)
       double wDiff = newWidth - oldWidth;
       double newX = node.getLayoutBounds().getMinX() + wDiff;
       node.relocate(newX, 0);
   }
   ```
   </details>
   <br >
   
   Some notes on this implementation:  
   1) It's crucial to also invalidate the hPos, because when a column becomes smaller it may happen to have a smaller virtualMaxX value.
   2) The `partialLayout(...)` method is exclusive to the `VFXVariableTableHelper`
   3) It assumes the usage of a `StateMap` to store the cells

// TODO additional optimizations. Visibility

### _To generate or not generate a new state..._

For some changes it's not necessary to generate a new state object. A new one, for example, is tipically needed when one
or both the ranges change, but this is not always the case.  
When a column changes its factory, or its width, for example, the state is technically the same because: in the first case
it's the rows' state that is changing, while in the latter it's a layout change.  
<br >

#### _So, the question is, should we still generate and set a new state?_
The idea would be to always communicate to the user that something in the system changed. That said, there are a series of
things to consider:
- A new flag should be added to the state class, a boolean value to tell that the state is technically a "clone" of
  a previous one.
- The creation of the state should be fast. Rather than moving things around, it is desirable to create a new object with
  the **references** from the other state.
- **In alternative** to the above solution, we could set the aforementioned flag to true and force the trigger of the
  JavaFX's `fireValueChangedEvent()` method.
- There's a listener in the table's skin (well in every container's skin to be precise) that listens to state changes and
  updates both the children's lists and the layout. In case a new state is a **"clone"** (check by using the aforementioned flag)
  then we should not perform any operation in the listener.
