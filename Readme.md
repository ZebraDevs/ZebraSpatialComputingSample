# Introduction

The ZebraSpatialComputingSample application is a basic example of how to
develop an augmented reality (AR) application using ARCore, Sceneform,
and DataWedge APIs on Zebra\'s Android devices. The app demonstrates the
foundational elements for a Realogram and Planogram compliance workflow,
using a barcode scanner to identify items on a shelf. It includes sample
code that showcases the collection and storage of barcode data and their
spatial coordinates relative to each other. This data is stored in a
local database and can be displayed as AR placards through the camera
viewfinder. Once the database is captured these AR assets can be
restored by scanning a reference barcode anywhere in the scene. Lastly,
the application features a \"playground\" function named \"Basic AR
Playground\" that allows users to interact with AR assets through the
camera viewfinder demonstrating a variety of helper functions that
developers can make use of to accelerate application development.

# Built With

To get started, use Android Studio version 3.1 or higher with Android
SDK Platform version 7.0 (API level 24) or higher. The application is
built using the ARCore SDK and Sceneform SDK.

ARCore is Google\'s platform for building augmented reality (AR)
experiences for Android devices. ARCore provides developers with tools
and APIs to create AR applications that blend digital content with the
real world. It enables the development of interactive and immersive AR
apps that can run on a wide range of Android devices.

Sceneform was a 3D framework developed by Google to make it easier for
developers to work with AR (Augmented Reality) and 3D content in Android
applications.

Room is used as helper to simplify database access. Room is a
persistence library provided by Google as part of the Android Jetpack
set of libraries. It provides an abstraction layer over SQLite, which is
the default database engine used on Android.

The language of development is Kotlin.

# Getting Started

## Prerequisites

The ZebraSpatialComputingSample source code is compiled with ArCore
1.36.0 Version. Certainly, the application requires Google Play Services
for AR Version 1.36.0 or higher to run on target devices. First time
when the application is launched, grant permissions to access Camera and
Storage.

## Target Devices

ZebraSpatialComputingSample Application is targeted to work on ARCore
supported Android devices. Zebra validated this application working on
TC58, TC53 and TC52 devices.

# Features

## Realogram / Planogram Workflow

Augmented Reality allows the ability to present workflow directions and
instructions to workers in the field of view on their AR-enabled mobile
devices using AR assets attached to their real-world location.

In this example, users may find AR especially beneficial in creating a
Realogram of the section by first scanning the section barcode to
geo-register AR session and then scanning product barcodes to create
child nodes relative to the section barcode.

### Generate Planogram

This feature highlights usage of AR to create Digital Realogram. Digital
Realogram is a JSON representation depicting the arrangement of products
within a module or section.

1.  Start the application, choose "Generate Planogram" option from the
    Navigation Drawer.

2.  Wait for a Vertical Plane to be detected. This is indicated by the
    appearance of white dots on the screen.

3.  Scan the "Section Barcode" using imager, by pressing the yellow
    button on the side of the device. "Section Barcode" is the
    barcode/QR code stuck to one corner of the section/module that
    provides information about the section. A virtual banner appears on
    the screen, on successful scan.

4.  Then scan all the product barcodes in the section. As you scan the
    products, product (child) banners appear on the screen.

5.  When all the products are scanned in press "Save Locations" button,
    to create a planogram.json file in application's filesDir.

### Pick Products using Planogram

1.  It is prerequisite that planogram.json file exists in application
    FilesDir is present for this feature to work. This can be either
    created manually by measuring the relative distance in meters of
    every product with respect to the section barcode or by using the
    feature "Create Realogram" feature explained above.

2.  Start the application, choose "Restore Planogram" option
    from the Navigation Drawer.

3.  Wait for a Vertical Plane to be detected. This is indicated by the
    appearance of white dots on the screen.

4.  Scan the "Section Barcode" using imager, by pressing the yellow
    button on the side of the device. "Section Barcode" is the
    barcode/QR code stuck to one corner of the section/module that
    provides information about the section. Virtual banners appear on
    the screen, on successful scan for section barcode and all the
    product barcodes relative to the section barcode.

## Basic AR Playground

Basic AR Playground allows the user to understand the core features of
Augmented Reality like plane finding, creating, and placing virtual
objects in the viewfinder, interacting with virtual objects, and
creating child virtual objects. This mode will display any detected
planes and will allow the user to tap on a plane to place a 3D model
(banner). It also allows the user to create child nodes relative to the
parent banner placed by tapping on the screen.

# Key Components

The virtual assets are represented as Node objects which contain the
local and world pose of the object, as well as the "Renderable" to be
attached to the node derived from a 3D or 2D (view). Each node has a
parent (either the scene itself or another node). The local pose is
relative to the parent. You can anchor a node to a plane (horizontal or
vertical), a feature point, or just scene itself depending on your use
case.

The "pose" of a node in an AR session is relative to a world coordinate
system unique to that session (as long AR is "tracking"). When you
create a new session (or lose track and not recover), this world
coordinate system will have changed, and the node pose needs to be
remapped from the old to the new coordinate system.

We use the scanning of "Section Barcode" to geo-register the current
session of AR Core. During "Create Planogram" workflow, we save the relative
position of products with respect to geo-registered section barcode in
the planogram file. During "Restore Planogram" workflow, when the user scans the
section barcode to geo-register the session, we can recreate all child
nodes using the relative positions saved in the planogram file.

## Scan to Place a Banner

A barcode banner is created at the intersection of the vertical plane
found by ARCore and the Zebra barcode scanner line of sight i.e., the
barcode position in the real world.

## Create Child Banner Relative to Parent

Creates a child node at the specified offset from the current node,
displaying the offset as a text banner.

## Save and Retrieve Node Information

Currently we save product locations with respect to section barcode
using Room Database and write to planogram.json file. The same concept
can be scaled to include multiple sections in a store.
