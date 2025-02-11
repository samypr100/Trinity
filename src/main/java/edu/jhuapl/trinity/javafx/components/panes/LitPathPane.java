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

import edu.jhuapl.trinity.utils.ResourceUtils;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Text;
import javafx.util.Duration;
import lit.litfx.controls.covalent.PathPane;
import lit.litfx.controls.covalent.events.CovalentPaneEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.scene.effect.PerspectiveTransform;

/**
 * @author Sean Phillips
 */
public class LitPathPane extends PathPane {
    Scene scene;
    Pane parent;
    boolean fadeEnabled = true;
    double fadeSideInset = -40;
    double hoverTopInset = -2;
    double hoverSideInset = -38;
    public Color fillPreStartColor = Color.CADETBLUE;
    public Color fillStartColor = Color.TRANSPARENT;
    public Color fillMiddleColor = Color.CYAN;
    public Color fillEndColor = Color.TRANSPARENT;
    public Color fillPostEndColor = Color.VIOLET;
    public LinearGradient lg;
    public Stop stop1, stop2, stop3;
    public SimpleDoubleProperty percentComplete = new SimpleDoubleProperty(0.0);
    private Timeline gradientTimeline;
    private double currentGradientMillis = 465; //This number was picked by Josh
    private long lastInsideMillis = 0;
    public static long enteredWaitTimeMillis = 5000; 
    
    /**
     * Helper utility for loading a common FXML based Controller which assumes
     * an anchorpane node which is returned wrapped as a BorderPane
     *
     * @param controllerLocation The path to the FXML file to load. e.g.
     *                           "/edu/jhuapl/trinity/fxml/ManifoldControl.fxml"
     * @return BorderPane the userContent
     */
    public static BorderPane createContent(String controllerLocation) {
        //make transparent so it doesn't interfere with subnode transparency effects
        Background transBack = new Background(new BackgroundFill(
            Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY));
        FXMLLoader loader = new FXMLLoader(LitPathPane.class.getResource(controllerLocation));
        loader.setLocation(LitPathPane.class.getResource(controllerLocation));
        BorderPane sgRoot;
        try {
            AnchorPane anchorPane = loader.load();
            sgRoot = new BorderPane(anchorPane);
            sgRoot.setBackground(transBack);
        } catch (IOException ex) {
            sgRoot = new BorderPane(new Text("Unable to load FXML Controller: " + controllerLocation));
            Logger.getLogger(LitPathPane.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sgRoot;
    }
    public static void slideInPane(PathPane pane) {
        //https://stackoverflow.com/questions/48893282/javafx-apply-perspective-transformation-on-a-node-given-a-perspective-transform?noredirect=1&lq=1
        PerspectiveTransform pt = new PerspectiveTransform();
        pt.setUlx(pane.getWidth());
        pt.setUly(pane.getHeight() * 0.5);
        pt.setUrx(pane.getWidth() + 1.0);
        pt.setUry(pane.getHeight() * 0.5);

        pt.setLrx(pane.getWidth() + 1.0);
        pt.setLry(pane.getHeight() * 0.5);
        pt.setLlx(pane.getWidth());
        pt.setLly(pane.getHeight() * 0.5);
        pane.setEffect(pt);

        Duration showPointDuration = Duration.seconds(0.75);
        Duration midPointDuration = Duration.seconds(0.75);
        Duration endPointDuration = Duration.seconds(1.00);

        Timeline timeline = new Timeline(
            new KeyFrame(showPointDuration, e -> pane.show()),

            //animation to midpoint
            new KeyFrame(midPointDuration, new KeyValue(pt.ulxProperty(), pane.getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(midPointDuration, new KeyValue(pt.urxProperty(), pane.getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.uryProperty(), pane.getHeight() * 0.333)),

            new KeyFrame(midPointDuration, new KeyValue(pt.lrxProperty(), pane.getWidth())),
            new KeyFrame(midPointDuration, new KeyValue(pt.lryProperty(), pane.getHeight() * 0.666)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llxProperty(), pane.getWidth() * 0.75)),
            new KeyFrame(midPointDuration, new KeyValue(pt.llyProperty(), pane.getHeight())),

            //animation to actual size
            new KeyFrame(endPointDuration, new KeyValue(pt.ulxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.ulyProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.urxProperty(), pane.getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.uryProperty(), 0.0)),

            new KeyFrame(endPointDuration, new KeyValue(pt.lrxProperty(), pane.getWidth())),
            new KeyFrame(endPointDuration, new KeyValue(pt.lryProperty(), pane.getHeight())),
            new KeyFrame(endPointDuration, new KeyValue(pt.llxProperty(), 0.0)),
            new KeyFrame(endPointDuration, new KeyValue(pt.llyProperty(), pane.getHeight()))
        );
        timeline.play();
        timeline.setOnFinished(e -> {
            pane.setEffect(null);
        });
    }
    public LitPathPane(Scene scene, Pane parent, int width, int height, Pane userContent, String mainTitleText, String mainTitleText2, double borderTimeMs, double contentTimeMs) {
        super(scene, parent, width, height, userContent, mainTitleText, mainTitleText2, borderTimeMs, contentTimeMs);
        this.scene = scene;
        this.parent = parent;
        // must be set to prevent user from resizing too small.
        setMinWidth(300);
        setMinHeight(200);
        setEffects();
    }

    private void setEffects() {
        ImageView iv = ResourceUtils.loadIcon("fade", 50);
        Label labelFadeout = new Label("Fadeout", iv);
        Border activeBorder = new Border(new BorderStroke(
            Color.CYAN, BorderStrokeStyle.DOTTED,
            CornerRadii.EMPTY, new BorderWidths(1), new Insets(0, fadeSideInset, 0, fadeSideInset))
        );
        Border hoverBorder = new Border(new BorderStroke(
            Color.WHITE, BorderStrokeStyle.SOLID,
            CornerRadii.EMPTY, new BorderWidths(1),
            new Insets(hoverTopInset, hoverSideInset, hoverTopInset, hoverSideInset))
        );

        AnchorPane.setBottomAnchor(labelFadeout, -16.0);
        AnchorPane.setRightAnchor(labelFadeout, 40.0);
        this.mainContentBorderFrame.getChildren().add(labelFadeout);
        Glow glow = new Glow(0.9);

        labelFadeout.setOnMouseEntered(e -> labelFadeout.setBorder(hoverBorder));
        labelFadeout.setOnMouseExited(e -> {
            if (fadeEnabled)
                labelFadeout.setBorder(activeBorder);
            else
                labelFadeout.setBorder(null);
        });

        Background background = new Background(new BackgroundFill(
            Color.CYAN.deriveColor(1, 1, 1, 0.1),
            CornerRadii.EMPTY, new Insets(0, fadeSideInset, 0, fadeSideInset)));
        labelFadeout.setEffect(glow);
        labelFadeout.setBackground(background);
        labelFadeout.setOnMouseClicked(e -> {
            fadeEnabled = !fadeEnabled;
            if (fadeEnabled) {
                labelFadeout.setEffect(glow);
                labelFadeout.setBackground(background);
                labelFadeout.setBorder(activeBorder);
            } else {
                labelFadeout.setEffect(null);
                labelFadeout.setBackground(null);
                labelFadeout.setBorder(null);
            }
        });

        this.scene.getRoot().addEventHandler(CovalentPaneEvent.COVALENT_PANE_CLOSE, e -> {
            if (e.pathPane == this)
                parent.getChildren().remove(this);
        });
        this.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> this.toFront());
        gradientTimeline = setupGradientTimeline();
        //transparency fade effects...
        addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            if (fadeEnabled) {
                fade(100, 0.8);
                if(System.currentTimeMillis()-lastInsideMillis >enteredWaitTimeMillis){             
                    gradientTimeline.setCycleCount(1);
                    gradientTimeline.setAutoReverse(false);
                    gradientTimeline.playFromStart();
                }
            } else
                contentPane.setOpacity(0.8);
        });
        addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            lastInsideMillis = System.currentTimeMillis();
            if (fadeEnabled) {
                fade(100, 0.3);
                this.outerFrame.setFill(Color.TRANSPARENT);
            } else
                contentPane.setOpacity(0.8);
        });
    }

    private Timeline setupGradientTimeline() {
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(30), new KeyValue(percentComplete, 0.0)),
            new KeyFrame(Duration.millis(currentGradientMillis), new KeyValue(percentComplete, 1.0))
        );
        timeline.setOnFinished(e -> {
            percentComplete.set(0);
            setGradientByComplete(); //will reset the gradient but not set fill
            outerFrame.setFill(Color.TRANSPARENT);
        });
        percentComplete.addListener(l -> updateGradient());
        return timeline;
    }

    private void setGradientByComplete() {
        Stop preStopClear = new Stop(percentComplete.get() - 0.15, Color.TRANSPARENT);
        Stop preStop = new Stop(percentComplete.get() - 0.1, fillPreStartColor);
        stop1 = new Stop(percentComplete.get() - 0.01, fillStartColor);
        stop2 = new Stop(percentComplete.get(), fillMiddleColor);
        stop3 = new Stop(percentComplete.get() + 0.01, fillEndColor);
        Stop postStop = new Stop(percentComplete.get() + 0.1, fillPostEndColor);
        Stop postStopClear = new Stop(percentComplete.get() + 0.15, Color.TRANSPARENT);
        ArrayList<Stop> stops = new ArrayList<>();
        stops.add(preStopClear);
        stops.add(preStop);
        stops.add(stop1);
        stops.add(stop2);
        stops.add(stop3);
        stops.add(postStop);
        stops.add(postStopClear);
        lg = new LinearGradient(
            0.5, 1.0, 0.5, 0.0, true, CycleMethod.NO_CYCLE, stops);
    }

    private void updateGradient() {
        setGradientByComplete();
        this.outerFrame.setFill(lg);
    }

    public void fade(double timeMS, double toValue) {
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(timeMS), contentPane);
        fadeTransition.setToValue(toValue);
        fadeTransition.setOnFinished(e -> contentPane.setOpacity(toValue));
        fadeTransition.play();
    }
}
