[//]: @formatter:off

# VFXGrid

## Specs

- Style Class: `vfx-grid`
- Default Skin: `VFXGridSkin`
- Default Behavior: `VFXGridManager`

### Properties

| Properties               | Description                                                                           | Type           |
|--------------------------|---------------------------------------------------------------------------------------|----------------|
| 1) items                 | Specifies the list containing the items to display                                    | ObservableList |
| size(delegate)           | Specifies the size of the list                                                        | Integer        |
| empty(delegate)          | Specifies whether the list is empty                                                   | Boolean        |
| 2) cellFactory           | Specifies the function used to build the Cells                                        | Function       |
| helper                   | Specifies the VFXGridHelper instance, a utility class which defines core computations | VFXGridHelper  |
| helperFactory            | Specifies the function used to build the VFXGridHelper                                | Supplier       |
| 3) vPos                  | Specifies the viewport's vertical position                                            | Double         |
| 3) hPos                  | Specifies the viewport's horizontal position                                          | Double         |
| 4) virtualMaxX(delegate) | Specifies the total number of pixels alongside the x-axis                             | Double         |
| 4) virtualMaxY(delegate) | Specifies the total number of pixels alongside the y-axis                             | Double         |
| state                    | Specifies the state object, which represents the current grid's state                 | VFXGridState   |
| needsViewportLayout      | Specifies whether the viewport's layout needs to be computed                          | Boolean        |

### Styleable Properties

| Property         | Description                                                                               | CSS Property            | Type             | Default Value  |
|------------------|-------------------------------------------------------------------------------------------|-------------------------|------------------|----------------|
| 5) cellSize      | Specifies the width and height of the cells                                               | -vfx-cell-size          | Size             | [100.0, 100.0] |
| 6) columnsNum    | Specifies the maximum number of columns the grid can have                                 | -vfx-columns-num        | Integer          | 5              |
| alignment        | Specifies the position of the viewport node inside the grid                               | -vfx-alignment          | Pos              | TOP_LEFT       |
| vSpacing         | Specifies the number of pixels between each row                                           | -vfx-v-spacing          | Double           | 0.0            |
| hSpacing         | Specifies the number of pixels between each column                                        | -vfx-h-spacing          | Double           | 0.0            |
| clipBorderRadius | Specifies the radius of the clip applied to the grid (to avoid cells overflow)            | -vfx-clip-border-radius | Double           | 0.0            |
| cacheCapacity    | Specifies the maximum number of cells to keep in the grid's cache when not needed anymore | -vfx-cache-capacity     | Integer          | 10             |
| 7) bufferSize    | Specifies the extra number of cells to render (makes scrolling smoother)                  | -vfx-buffer-size        | Enum(BufferSize) | Standard(2)    |

### Additional capabilities

- Auto arrange (determine number of columns based on width)

## Internals

_**Architecture**_
- Grid (Model): defines the capabilities/features
- Grid Skin (View): mostly handles layout
- Grid Manager (Behavior/Controller/Manager): reacts to inputs and properties changes to produce new states

<br >

**How does it work?**  
First and foremost, it's important to keep in mind that the Grid operates on a **one dimension data structure**, a simple list.
This means that it needs a fundamental value for it to function: **the number of columns**. This property can be implemented
in two ways: the first is to use the cell size and the container's width to compute the value dynamically;
the second is to simply let the user set the desired value. (both approaches are implemented btw)  
This approaches allow us to make a very versatile skin. The viewport node can be sized according to the number of columns and
the cell size. The benefit is that through a property that specifies the alignment, we can position it as we desire in
the container. For example we could have a gallery with a certain number of columns,and have the viewport to be aligned
at the center, thus having equal spacing at the sides.  
However, having a property specifying the number of columns, doesn't mean that the virtualization is disabled on the x-axis.
If the number of columns is high enough to surpass the needed number, then the container should only render the ones needed.  
_So, how many values for the number of columns do we have?_  
- The **desired number** coming from the property
- The **visible number** coming from the container's width and cell size
- The **total number**, which is the visible number plus double the buffer
- The **final number** should be a value which is lesser than or equal to the desired one and the number of items.

The **number of rows** depend on the number of columns.

<br >

_**On changes/events/actions**_  
_Note 1: Numbers above and below are correlated ofc_  
_Note 2: Every **change** should produce and set a **new state**_  
_Note 3: The following "explanations" should not be considered the real implementation as in reality things may differ.
These are more like dev notes that helped me build the system by figuring out the core concepts/mechanisms._

0) Init depends on geometry (width/height). It's simple as it's enough to ensure that the viewport has the right amount of cells.
   Old cells do not need to update, just disposal or caching (in case there are too many)
   It's also important to ensure the positions are valid!
1) Very similar/same algorithm used for the List.
2) Recreate cells: Purge and dispose cached/old cells: No need to recompute positions and sizes.
3) Update cells.
4) This typically changes only as a consequence of others changing.
5) Ensure correct number of cells; Update 4; Ensure valid position; Layout.
6) When the number of columns changes, there are many others following. Update 4; Ensure valid positions;
   Both the ranges (rows/columns) may change. Depending on the container's size, the number of cells may change too.
   Because of this, maybe this could be treated as a geometry change.
7) Virtualized containers should have buffer cells both at the top and bottom of the viewport for a smoother scroll experience. 
   When the number of buffer cells changes, it's enough to act as if the geometry changed since only one thing can happen:
   more or less cells than needed.