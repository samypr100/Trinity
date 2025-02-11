package edu.jhuapl.trinity.javafx.components.panes;

/*-
 * #%L
 * trinity
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import edu.jhuapl.trinity.data.messages.FeatureVector;
import edu.jhuapl.trinity.javafx.components.callouts.Callout;
import edu.jhuapl.trinity.javafx.components.callouts.FeatureVectorCallout;
import edu.jhuapl.trinity.javafx.components.radial.RadialEntity;
import edu.jhuapl.trinity.javafx.events.ApplicationEvent;
import edu.jhuapl.trinity.javafx.events.CommandTerminalEvent;
import edu.jhuapl.trinity.utils.HttpsUtils;
import edu.jhuapl.trinity.utils.JavaFX3DUtils;
import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape3D;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static java.util.stream.Collectors.toList;

/**
 * @author Sean Phillips
 */
public class RadialEntityOverlayPane extends Pane {
    public static double CHIP_FIT_WIDTH = 200;
    public static double IMAGE_FIT_HEIGHT = 200;
    public static double IMAGE_FIT_WIDTH = 200;
    public static double DEFAULT_FADE_TIMEMS = 500;
    public static double DEFAULT_GAP = 10.0;
    public SimpleBooleanProperty showing = new SimpleBooleanProperty(false);
    public BorderPane borderPane;
    public StackPane centerStack;
    public Scene scene;
    HashMap<FeatureVector, RadialEntity> vectorToEntityMap;
    HashMap<FeatureVector, Callout> vectorToCalloutMap;
    HashMap<Shape3D, Callout> shape3DToCalloutMap;

    ObservableList<RadialEntity> entityList;
    ObservableList<Callout> calloutList;
    public List<FeatureVector> featureVectors;
    public String imageryBasePath = "imagery/";

    public RadialEntityOverlayPane(Scene scene, List<FeatureVector> featureVectors) {
        setBackground(Background.EMPTY);
        getStyleClass().add("radial-entity-overlay-pane");
        this.scene = scene;
        this.featureVectors = featureVectors; //shared resource. Hacky I know.
        centerStack = new StackPane();
        centerStack.setAlignment(Pos.CENTER);
        centerStack.minWidthProperty().bind(widthProperty());
        centerStack.minHeightProperty().bind(heightProperty());
        centerStack.maxWidthProperty().bind(widthProperty());
        centerStack.maxHeightProperty().bind(heightProperty());
        getChildren().addAll(centerStack);

        setPickOnBounds(false); //prevent it from blocking mouse clicks to sublayers
        centerStack.setPickOnBounds(false);
        shape3DToCalloutMap = new HashMap<>();
        entityList = FXCollections.observableArrayList();
        vectorToEntityMap = new HashMap<>();
        calloutList = FXCollections.observableArrayList();
        vectorToCalloutMap = new HashMap<>();
        scene.addEventHandler(ApplicationEvent.SET_IMAGERY_BASEPATH, e -> {
            imageryBasePath = (String) e.object;
            System.out.println("Callout image base path set to " + imageryBasePath);
        });
    }

    public void addEntity(RadialEntity radialEntity) {
        entityList.add(radialEntity);
        getChildren().add(radialEntity);
        getChildren().add(radialEntity.centerBe);
    }

    public void removeEntity(RadialEntity radialEntity) {
        entityList.remove(radialEntity);
        getChildren().remove(radialEntity);
    }

    public void removeEntity(FeatureVector featureVector) {
        RadialEntity re = vectorToEntityMap.get(featureVector);
        entityList.remove(re);
        getChildren().remove(re);
        vectorToEntityMap.remove(featureVector);
    }

    public void clearEntities() {
        entityList.clear();
        getChildren().removeIf(node -> node instanceof RadialEntity);
        vectorToEntityMap.clear();
    }

    public Callout createCallout(Shape3D shape3D, FeatureVector featureVector, SubScene subScene) {
        Callout infoCallout = FeatureVectorCallout.createByShape3D(shape3D, featureVector, subScene);
        addCallout(infoCallout, shape3D);
        infoCallout.play();
        return infoCallout;
    }

    public void updateCalloutByFeatureVector(Callout callout, FeatureVector featureVector) {
        //UPdate label
        callout.setMainTitleText(featureVector.getLabel());
        callout.mainTitleTextNode.setText(callout.getMainTitleText());
        //update image (incoming hypersonic hack)
        VBox vbox = (VBox) callout.mainTitleNode;
        TitledPane tp0 = (TitledPane) vbox.getChildren().get(0);
        ImageView iv = loadImageView(featureVector, featureVector.isBBoxValid());
        Image image = iv.getImage();
        ((ImageView) tp0.getContent()).setImage(image);

        //update details (child 2)
        String bboxStr = "";
        if (null != featureVector.getBbox())
            bboxStr = bboxToString(featureVector);
        TitledPane tp1 = (TitledPane) vbox.getChildren().get(2);
        GridPane detailsGridPane = new GridPane();
        detailsGridPane.setPadding(new Insets(1));
        detailsGridPane.setHgap(5);
        detailsGridPane.addRow(0, new Label("imageURL"),
            new Label(featureVector.getImageURL()));
        detailsGridPane.addRow(1, new Label("bbox"),
            new Label(bboxStr));
        detailsGridPane.addRow(2, new Label("frameId"),
            new Label(String.valueOf(featureVector.getFrameId())));
        detailsGridPane.addRow(3, new Label("score"),
            new Label(String.valueOf(featureVector.getScore())));
        detailsGridPane.addRow(4, new Label("layer"),
            new Label(String.valueOf(featureVector.getLayer())));
        detailsGridPane.addRow(5, new Label("messageId"),
            new Label(String.valueOf(featureVector.getLayer())));
        tp1.setContent(detailsGridPane);

        //update metadata (child 3)
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : featureVector.getMetaData().entrySet()) {
            sb.append(entry.getKey()).append(" : ").append(entry.getValue()).append("\n");
        }
        TitledPane tp2 = (TitledPane) vbox.getChildren().get(3);
        ((Text) tp2.getContent()).setText(sb.toString());
    }

    public List<FeatureVector> getFeatureVectorsByImage(FeatureVector featureVector) {
        return featureVectors.stream()
            .filter(fv -> fv.getImageURL().contentEquals(featureVector.getImageURL()))
            .collect(toList());
    }

    private String bboxToString(FeatureVector featureVector) {
        NumberFormat format = new DecimalFormat("0.00");
        StringBuilder sb = new StringBuilder("[ ");
        for (Double d : featureVector.getBbox()) {
            sb.append(format.format(d));
            sb.append(" ");
        }
        sb.append("]");
        String bboxStr = sb.toString();
        return bboxStr;
    }

    private ImageView loadImageView(FeatureVector featureVector, boolean bboxOnly) {
        if (null == featureVector.getImageURL())
            return new ImageView(ResourceUtils.loadIconFile("noimage"));
        ImageView iv = null;
        try {
            if (featureVector.getImageURL().startsWith("http")) {
                //@DEBUG SMP Useful print
                //System.out.println("<Trinity Debug> HTTP Link: fv.getImageURL()== " + featureVector.getImageURL());
                Image image = HttpsUtils.getImage(featureVector.getImageURL());
                if (image.getException() != null)
                    System.out.println("Exception info: " + image.getException().toString());
                iv = new ImageView(image);
            } else {
                if (bboxOnly) {
                    //@DEBUG SMP Useful print
                    //System.out.println("<Trinity Debug> BoundingBox Request: file == " + imageryBasePath + featureVector.getImageURL());
                    WritableImage image = ResourceUtils.loadImageFileSubset(imageryBasePath + featureVector.getImageURL(),
                        featureVector.getBbox().get(0).intValue(),
                        featureVector.getBbox().get(1).intValue(),
                        featureVector.getBbox().get(2).intValue(),
                        featureVector.getBbox().get(3).intValue()
                    );
                    iv = new ImageView(image);
                } else {
                    //@DEBUG SMP Useful print
                    //System.out.println("<Trinity Debug> Full Image Request: file == " + imageryBasePath + featureVector.getImageURL());
                    iv = new ImageView(ResourceUtils.loadImageFile(imageryBasePath + featureVector.getImageURL()));
                }
            }
        } catch (Exception ex) {
            System.out.println("Oops... problem getting image! " + ex.getMessage());
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        }
        if (iv == null)
            iv = new ImageView(ResourceUtils.loadIconFile("noimage"));
        return iv;
    }

    public void updateCalloutHeadPoint(Shape3D node, Callout callout, SubScene subScene) {
        Point2D p2d = JavaFX3DUtils.getTransformedP2D(node, subScene, callout.head.getRadius() + 5);
        callout.updateHeadPoint(p2d.getX(), p2d.getY());
    }

    public void updateCalloutHeadPoints(SubScene subScene) {
        shape3DToCalloutMap.forEach((node, callout) -> {
            updateCalloutHeadPoint(node, callout, subScene);
        });
    }

    public void addCallout(Callout callout, Shape3D shape3D) {
        calloutList.add(callout);
        callout.setManaged(false);
        getChildren().add(callout);
        //Anchor mapping for callout in 3D space
        shape3DToCalloutMap.put(shape3D, callout);
    }

    public void removeCallout(FeatureVector featureVector) {
        Callout callout = vectorToCalloutMap.get(featureVector);
        calloutList.remove(callout);
        getChildren().remove(callout);
        vectorToCalloutMap.remove(featureVector);
    }

    public void clearCallouts() {
        calloutList.clear();
        getChildren().removeIf(node -> node instanceof Callout);
        vectorToCalloutMap.clear();
        shape3DToCalloutMap.clear();
    }

    public void hide(double timeMS) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), this);
        fadeTransition.setToValue(0);
        fadeTransition.setOnFinished(e -> {
            setOpacity(0.0);
            getScene().getRoot().fireEvent(new CommandTerminalEvent(
                "Radial Overlay Disengaged.", new Font("Consolas", 20), Color.GREEN));
            showing.set(false);
        });
        fadeTransition.play();
    }

    public void show(double timeMS) {
        setVisible(true);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), this);
        fadeTransition.setToValue(1);
        fadeTransition.setOnFinished(e -> {
            setOpacity(1.0);
            getScene().getRoot().fireEvent(new CommandTerminalEvent(
                "Radial Overlay Engaged.", new Font("Consolas", 20), Color.GREEN));
            showing.set(true);
        });
        fadeTransition.play();
    }
}
