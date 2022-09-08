# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).  
(Date format is dd-MM-yyyy)

## Type of Changes

- **Added** for new features.
- **Changed** for changes in existing functionality.
- **Deprecated** for soon-to-be removed features.
- **Removed** for now removed features.
- **Fixed** for any bug fixes.

## [11.5.1] - 08-09-2022

<h4>
Note that the changelog for this version also includes the below "Commits".
This to have a complete log of the changes that lead to this new version.
</h4>

## Changed

- FlowState: I realized that there's no need to store cells' position is a map and keep them sorted when needed. Now
  positions are kept in a TreeSet, the VirtualFlowSkin then uses two ListIterators to iterate at the same time on both
  the cells and the positions (from bottom, so both iterators go in reverse)

## Fixed

- FlowState: further improve the new addition algorithm. Fixed a bunch of bugs related to the last index computation, to
  the wrong computation of the targetSize for PaginatedVirtualFlows and to the wrong creation of PartialMappings when a
  FullMapping was needed instead

## [Commit] - 07-09-2022

## Changed

- Bump MFXCore to version 11.0.16
- Renamed ViewportState to FlowState
- Renamed StateProperty to FlowStateProperty
- VirtualFlowSkin: move itemsChanged listener code to protected method, needed by PaginatedVirtualFlowSkin subclass
- VirtualFlowSkin: move listChanged listener code to protected method, needed by PaginatedVirtualFlowSkin
- VirtualFlowSkin: adapted layout code to changes made to FlowState
- FlowState: inverted the layoutMap to Double -> Cell. This to make use of a TreeMap to always keep the positions
  ordered in reverse, thus avoiding a Collections.sort() in the computePositions() method

## Fixed

- MFXScrollBarSkin: fixed a bug that caused the thumb to be positioned outside the track in some specific occasions
- VirtualScrollPane: Sometimes it's also needed to invalidate the position of the virtual flow when the length changes
- PaginatedVirtualFlowSkin: sometimes the number of pages was not updated by listening on the estimatedLength property
  for some reason. Fixed by doing it every time the items list changes or the list changes
- FlowState: further testing proved that even the new algorithm to manage additions to the items list was still failing
  for both VirtualFlow and PaginatedVirtualFlow. Switched to a "mapping" technique which should finally work for both
- FlowState: also fixed algorithm to handle replacements in the items list as it was misbehaving in some specific
  occasions

## [Commit] - 02-09-2022

## Added

- Add new tests for both ComparisonTestController and PaginationTestController

## Changed

- Bump MFXCore to version 11.0.15
- Override toString() method for VirtualBounds
- ViewportState: make compute positions methods return the layout map
- Renamed AbstractHelper to AbstractOrientationHelper
- Moved estimated length and max breadth properties to super class
- The helpers now do not need the ViewportManager in the constructor as it can be retrieved from the virtual flow.
  Improvement for the orientationHelperFactory since the user cannot access the manager
- VirtualFlow, adapt to the aforementioned changes. Also add protected method to set the orientation helper
- Moved the cellSize invalidation code to separate method that can be overridden by subclasses if needed
- Improved PaginatedVirtualFlow management of the current page and max page
- Adapt PaginatedHelper to the aforementioned changes
- PaginatedVirtualFlow override cellSizeChanged() method to use goToPage() instead of scrollToPixel()
- Improved animation of cells' background

## Fixed

- VirtualScrollPane: fixed scroll bars' value not updating when the estimated length was changing
- ViewportState: fixed state transition which lead to NullPointerException is some rare cases. Also covered some extra
  cases specifically for the PaginatedVirtualFlow
- ViewportState: the algorithm responsible for transitioning to a new state in case of ADDITIONS is the virtual flow
  list has been completely rewritten as the previous implementation had some bugs for the original virtual flow and was
  also inadequate for the PaginatedVirtualFlow (2 or more bugs were found for that)
- ViewportState: the isViewportFull() methods now behaves differently for the PaginatedVirtualFlow, counting only the
  visible cells
- ViewportState: the getFirst() methods now behaves differently for the PaginatedVirtualFlow, instead of using the
  range's min value it now asks the OrientationHelper to compute the first visible cell. This is because of an issue
  occurring when a page was not full (some cells hidden, not enough items)
- ViewportState: the getLast() method now is more heavyweight since the returned value is now computed by finding the
  max index in the cells map. This at the benefit of 100% accuracy for both virtual flow implementations
- PaginatedHelper: Add scrollToPixel() method to the "forbidden" methods to avoid issues
- PaginatedVirtualFlow: fixed an issue that caused the display of the wrong page when changing the cells per page
  property
- PaginatedVirtualFlowSkin: added a listener on the estimated length property to fix an issue that caused the max page
  to not update when the length changed
- PaginationTestController: removed MFXSpinner in favor of custom solution. Needs to be fixed asap in MaterialFX

## [11.5.0] - 24-08-2022

## Added

- Added paginated version of VirtualFlow
- Added VirtualBounds bean
- Added new methods to create and dispose a VirtualScrollPane wrapping a PaginatedVirtualFlow
- Added two methods to adjust the horizontal and vertical speed of any VirtualScrollPane
- Added clipBorderRadius property yo VirtualFlow too, if anyone intends to use it without a scroll pane. Skin adapted to
  this change
- Added new method to get an integer from user to ComparisonTestController
- Added new test to showcase PaginatedVirtualFlow

## Changed

- Upgraded MaterialFX jar with the latest local changes
- To make the VirtualScrollPane more independent of its content, it now uses VirtualBounds rather than Bounds to get the
  bounds of its content
- VirtualScrollPane now has 4 different properties to manage the speed of the two scroll bars individually
- VirtualScrollPaneSkin has been adapted to VirtualScrollPane changes
- VirtualScrollPane has been adapted to work with PaginatedVirtualFlow
- Adapted ViewportState to work with PaginatedVirtualFlow too
- Adapted the ComparisonTestController to changes made to VirtualScrollPane

## [11.4.0] - 18-08-2022

## Added

- Introduced CHANGELOG
- Added MaterialFX jar to libs temporarily to include all features from the staging branch
- Added convenience property for the viewport state
- Imported scroll bars and scroll pane from experimental MaterialFX (not available yet)
- Replacement for SimpleVirtualFlow, a completely new implementation
- Added a test which compares the old virtual flow implementation against the new one

## Changed

- Upgraded Gradle to version 7.5.1
- Move dependencies and plugins versions to gradle.properties
- Moved SimpleVirtualFlow and all its components to the "unused" package (for testing and comparisons)
- The test module now is divided in two main packages. The "interactive" package contains UI tests that the user can
  interact with. The "unit" package contains traditional unit tests

## Removed

- Removed both NumberRange and NumberRangeProperty as better alternatives come from MFXCore
- Utils package and all classes removed. Most if not all of them are now offered by MFXCore
- Removed SetsDiff and package
- Most of the old tests have been deleted














