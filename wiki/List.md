[//]: @formatter:off

# VFXList

## Specs

- Style Class: `vfx-list`
- Default Skin: `VFXListSkin`
- Default Behavior: `VFXListManager`

### Properties

| Property                 | Description                                                                           | Type           |
|--------------------------|---------------------------------------------------------------------------------------|----------------|
| 1) items                 | Specifies the list containing the items to display                                    | ObservableList |
| size(delegate)           | Specifies the size of the list                                                        | Integer        |
| empty(delegate)          | Specifies whether the list is empty                                                   | Boolean        |
| 2) cellFactory           | Specifies the function used to build the Cells                                        | Function       |
| helper                   | Specifies the VFXListHelper instance, a utility class which defines core computations | VFXListHelper  |
| helperFactory            | Specifies the function used to build the VFXListHelper                                | Function       |
| 3) vPos                  | Specifies the viewport's vertical position                                            | Double         |
| 3) hPos                  | Specifies the viewport's horizontal position                                          | Double         |
| 4) virtualMaxX(delegate) | Specifies the total number of pixels alongside the x-axis                             | Double         |
| 4) virtualMaxY(delegate) | Specifies the total number of pixels alongside the y-axis                             | Double         |
| state                    | Specifies the state object, which represents the current list's state                 | VFXListState   |
| needsViewportLayout      | Specifies whether the viewport's layout needs to be computed                          | Boolean        |

### Styleable Properties

| Property         | Description                                                                               | CSS Property            | Type              | Default Value |
|------------------|-------------------------------------------------------------------------------------------|-------------------------|-------------------|---------------|
| 5) cellSize      | Specifies the width/height of the cells                                                   | -vfx-cell-size          | Double            | 32.0          |
| 6) spacing       | Specifies the number of pixels between each cell                                          | -vfx-spacing            | Double            | 0.0           |
| 7) bufferSize    | Specifies the extra number of cells to render (makes scrolling smoother)                  | -vfx-buffer-size        | Enum(BufferSize)  | Standard(2)   |
| 8) orientation   | Specifies the list direction: vertical or horizontal                                      | -vfx-orientation        | Enum(Orientation) | Vertical      |
| 9) fitToViewport | Specifies whether the cells should have the same size as the viewport                     | -vfx-fit-to-viewport    | Boolean           | true          |
| clipBorderRadius | Specifies the radius of the clip applied to the list (to avoid cells overflow)            | -vfx-clip-border-radius | Double            | 0.0           |
| cacheCapacity    | Specifies the maximum number of cells to keep in the list's cache when not needed anymore | -vfx-cache-capacity     | Integer           | 10            |

## Internals

_**Architecture**_
- List (Model): defines the capabilities/features 
- List Skin (View): mostly handles layout
- List Manager (Behavior/Controller/Manager): reacts to inputs and properties changes to produce new states 

<br >

_**On changes/events/actions**_  
_Note 1: Numbers above and below are correlated ofc_  
_Note 2: Every **change** should produce and set a **new state**_  
_Note 3: The following "explanations" should not be considered the real implementation as in reality things may differ.
These are more like dev notes that helped me build the system by figuring out the core concepts/mechanisms._

0) **Init/Geometry:** init depends on geometry (width/height) and orientation. It's simple as it's enough to ensure that
   the viewport has the right amount of cells. Old cells do not need to update, just disposal or caching (in case there are too many)
   It's also important to ensure the positions are valid!
1) Two changes can occur. **Internal** and **property**, however in theory it should be possible to treat them the same way.  
   When items change, there are several cases to consider. First and foremost one thing we can check is if after the change
   the list is **empty**.  
   If the new size is **lesser** than before, then it's needed to invalidate the positions too, otherwise it's not needed.
   After ensuring we are at the correct position, the following actions should be in theory very easy, since the new approach
   will not take into account changes types and ranges anymore.  
   Compute the **new range**, iterate over it, get cells (reuse or create as needed), update them both for index and item,
   proceed as usual.
2) Recreate cells: Purge and dispose cached/old cells: No need to recompute positions and sizes.
3) Update cells
4) This typically changes only as a consequence of others changing
5) Ensure correct number of cells; Update 4; Ensure valid position; Layout
6) Same as 5.
7) Virtualized containers should have buffer cells both at the top and bottom of the viewport for a smoother scroll experience.
   When the number of buffer cells changes, it's enough to act as if the geometry changed since only one thing can happen:
   more or less cells than needed.
8) Update 4; Cells can be reused, ensure they are updated; Layout
9) Update 4; Ensure valid position; Layout