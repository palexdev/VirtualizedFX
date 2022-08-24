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














