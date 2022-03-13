# graph_visualizator
Android app to visualize in augmented reality 3D models of undirected graph.

The app allows the users to the MyDrive account.
It allows to download the files representing the 3D model in .glb format and to visualize it via ARCore framework.
Before utilizing the Drive API it is necessary to identify the app, generating a client ID.
To access MyDrive, visualize the files in it and download them an Activity class (FileActivity) has been created.
This Activity utilizes utility classes, written in Kotlin language.
The chosen file is download in the Download directory of the device.


![Screenshot1](./screenshot2.jpeg "Screenshot1")

### ARCore and Sceneform

The Activity class that handles the augmented reality is composed by an ARFragment, essential component of ARCore, whose 
duties are:
- Handle the access to the camera of the device
- Create the ARCore session
- Identify the planes of the physical world, allowing to change the position and dimension of the virtual element inserted in the scene


![Screenshot1](./screenshot2.jpeg "Screenshot1")

Now the Sceneform framework is used to load the 3D model and insert it in the real contest.
The buildModel(File file) method is called.
The glTF file is passed to the RenderableSource class.
The method ModelRenderable.builder() creates the 3D model represented by the glTF file contained in the RenderableSource object

```java
private void buildModel(File file) {
 RenderableSource renderableSource = RenderableSource
         .builder()
         .setSource(this, Uri.parse(file.getPath()), RenderableSource.SourceType.GLB)
         .setRecenterMode(RenderableSource.RecenterMode.ROOT)
         .build();
   ModelRenderable
           .builder()
           .setSource(this, renderableSource)
           .setRegistryId(file.getPath())
           .build()
           .thenAccept(modelRenderable -> {
               Toast.makeText(this, "Model built", Toast.LENGTH_SHORT).show();
               renderable = modelRenderable;
           });
}

```

This above is the code of the Java method buildModel(File file)

Now that the Renderable object has been created, we need to add it in the real contest.
The addObject() method determines in which point of the space the model should be inserted, then, it calls the
addNodeToScene(Anchor anchor, ModelRenderable renderable) method to add the model to the scene.



```java
 private void addObject() {
   Frame frame = fragment.getArSceneView().getArFrame();
   android.graphics.Point pt = getScreenCenter();
   List<HitResult> hits;
   
      if (frame != null) {
       hits = frame.hitTest(pt.x, pt.y);
       for (HitResult hit : hits) {
           Trackable trackable = hit.getTrackable();
           if (trackable instanceof Plane &&
                   ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
               addNodeToScene(hit.createAnchor(), renderable);
               break;
           }
       }
   }
}


}

```

```java
public void addNodeToScene(Anchor anchor, ModelRenderable renderable){
   AnchorNode anchorNode = new AnchorNode(anchor);
   TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
   node.setRenderable(renderable);
   node.setParent(anchorNode); 
   fragment.getArSceneView().getScene().addChild(anchorNode);
node.select();
}


```


