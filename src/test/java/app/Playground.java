/*
 * Copyright (C) 2026 Parisi Alessandro - alessandro.parisi406@gmail.com
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX)
 *
 * VirtualizedFX is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX. If not, see <http://www.gnu.org/licenses/>.
 */

package app;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.palexdev.mfxcore.utils.StringUtils;
import io.github.palexdev.mfxcore.utils.fx.CSSFragment;
import io.github.palexdev.mfxeffects.animations.Animations;
import io.github.palexdev.mfxeffects.animations.ConsumerTransition;
import io.github.palexdev.mfxresources.icon.MFXFontIcon;
import io.github.palexdev.virtualizedfx.cells.VFXSimpleTableCell;
import io.github.palexdev.virtualizedfx.cells.base.VFXTableCell;
import io.github.palexdev.virtualizedfx.controls.VFXScrollPane;
import io.github.palexdev.virtualizedfx.enums.ColumnsFillPolicy;
import io.github.palexdev.virtualizedfx.events.VFXContainerEvent;
import io.github.palexdev.virtualizedfx.table.VFXTable;
import io.github.palexdev.virtualizedfx.table.VFXTableColumn;
import io.github.palexdev.virtualizedfx.table.defaults.VFXSimpleTableColumn;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import src.model.User;

import static io.github.palexdev.mfxcore.observables.When.observe;
import static io.github.palexdev.mfxresources.utils.IconUtils.randomFAS;
import static src.model.User.users;

public class Playground extends Application {
    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        TestTable table = new TestTable(users(100));
        //table.addEmptyColumns(500);
        table.addEmptyColumns(10);
        table.autosizeColumns();
        resetColumns(table);
        table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
        //table.setColumnsSize(50, 32);
        //table.setColumnsFillPolicy(ColumnsFillPolicy.WEIGHTED);
        //setWeight(table.getColumns().getFirst(), AUTOSIZE_ONCE);
        //setWeight(table.getColumns().get(5), AUTOSIZE);

        VFXScrollPane vsp = table.makeScrollable();
        vsp.setSmoothScroll(true);
        root.setCenter(vsp);

        Label state = new Label();
        state.setStyle("-fx-font-family: monospace;");
        Runnable printState = () -> state.setText(table.columns().stream()
            .map(c -> "%-12s width=%6.1f  pref=%6.1f  weight=%d  toAutosize=%b".formatted(
                c.getText(), c.getWidth(), c.getUserPrefWidth(), VFXTable.getWeight(c), c.isMarkedForAutosize()
            ))
            .collect(Collectors.joining("\n")));
        table.columns().forEach(c -> {
            observe(printState, c.widthProperty(), c.userPrefWidthProperty(), c.getProperties()).listen();
        });

        Runnable printScroll = () -> {
            Region viewport = (Region) vsp.lookup(".viewport");
            double vpW = (viewport == null) ? -1 : viewport.getWidth();
            Insets padding = (viewport == null) ? Insets.EMPTY : viewport.getPadding();
            double vpInner = vpW - padding.getLeft() - padding.getRight();
            System.out.printf("""
                    pane=%.4f  viewport=%.4f  viewportInner=%.4f
                    table=%.4f  virtualMaxX=%.4f  snapSizeX=%.4f
                    maxHScroll=%.6f  hVisibleAmount=%.6f  renderScaleX=%.4f
                    %n""",
                vsp.getWidth(), vpW, vpInner,
                table.getWidth(), table.getVirtualMaxX(), table.snapSizeX(table.getVirtualMaxX()),
                table.getMaxHScroll(), vsp.getHorizontalVisibleAmount(),
                (table.getScene() == null || table.getScene().getWindow() == null) ?
                    -1 : table.getScene().getWindow().getRenderScaleX()
            );
        };

        Button autosize = new Button("Autosize columns");
        autosize.setOnAction(_ -> {
            table.autosizeColumns();
            printState.run();
        });
        Button reset = new Button("Reset columns");
        reset.setOnAction(_ -> {
            resetColumns(table);
            printState.run();
        });

        Button print = new Button("Print scroll state");
        print.setOnAction(_ -> printScroll.run());

        HBox controls = new HBox(16, new VBox(8.0, autosize, reset, print), state);
        controls.setPadding(new Insets(16, 32, 0, 32));
        root.setTop(controls);

        CSSFragment.applyOn("""
            .vfx-scroll-pane > .viewport {
              -fx-padding: 32px;
            }
            
            .vfx-table {
              -fx-border-color: grey;
              -fx-border-radius: 16px;
              -vfx-clip-border-radius: 32px;
            }
            
            .columns {
              -fx-border-color: transparent transparent grey transparent;
              -fx-paddind: 0px 8px;
            }
            
            .vfx-row {
              -fx-background-color: white;
              -fx-background-insets: 0px 2px 0px 0px;
            }
            
            .vfx-row:nth-child(even) {
              -fx-background-color: lightgray;
            }
            
            .vfx-column {
              -fx-border-color: transparent grey transparent transparent;
              -vfx-enable-overlay: false;
              -vfx-overlay-on-header: false;
              -vfx-icon-alignment: LEFT;
              -vfx-resizable: true;
              -fx-padding: 0px 8px;
            }
            
            .vfx-column:hover .overlay {
              -fx-background-color: rgba(0, 0, 255, 0.2);
            }
            
            .cell-base {
              -fx-padding: 0px 8px;
            }
            """, root);

        Scene scene = new Scene(root, 1024, 720);
        stage.setScene(scene);
        stage.show();
    }

    static void resetColumns(TestTable table) {
        ObservableList<VFXTableColumn<User, ? extends VFXTableCell<User>>> columns = table.columns();
        for (int i = 0; i < columns.size(); i++) {
            VFXTableColumn<User, ?> column = columns.get(i);
            column.setUserPrefWidth(-1);
            VFXTable.setWeight(column, i < 2 ? 1 : ColumnsFillPolicy.DEFAULT_WEIGHT);
        }
    }

    static class TestTable extends VFXTable<User> {
        public TestTable(ObservableList<User> items) {
            super(items);
            defaultColumns();
        }

        void defaultColumns() {
            int ICON_SIZE = 18;
            Color ICON_COLOR = Color.rgb(53, 57, 53);

            var firstNameColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("First name");
            firstNameColumn.setCellFactory(u -> factory(u, User::firstName));
            firstNameColumn.setGraphic(new MFXFontIcon("fas-user", ICON_SIZE, ICON_COLOR));

            var lastNameColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Last name");
            lastNameColumn.setCellFactory(u -> factory(u, User::lastName));
            lastNameColumn.setGraphic(new MFXFontIcon("fas-user", ICON_SIZE, ICON_COLOR));

            var birthColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Birth year");
            birthColumn.setCellFactory(u -> factory(u, User::birthYear));
            birthColumn.setGraphic(new MFXFontIcon("fas-cake-candles", ICON_SIZE, ICON_COLOR));

            var zodiacColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Zodiac Sign");
            zodiacColumn.setCellFactory(u -> factory(u, User::zodiac));
            zodiacColumn.setGraphic(new MFXFontIcon("fas-star", ICON_SIZE, ICON_COLOR));

            var countryColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Country");
            countryColumn.setCellFactory(u -> factory(u, User::country));
            countryColumn.setGraphic(new MFXFontIcon("fas-globe", ICON_SIZE, ICON_COLOR));

            var bloodColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Blood");
            bloodColumn.setCellFactory(u -> factory(u, User::blood));
            bloodColumn.setGraphic(new MFXFontIcon("fas-droplet", ICON_SIZE, ICON_COLOR));

            var animalColumn = new VFXSimpleTableColumn<User, VFXSimpleTableCell<User, ?>>("Pet");
            animalColumn.setCellFactory(u -> factory(u, User::pet));
            animalColumn.setGraphic(new MFXFontIcon("fas-paw", ICON_SIZE, ICON_COLOR));

            columns().addAll(firstNameColumn, lastNameColumn, birthColumn, zodiacColumn, countryColumn, bloodColumn, animalColumn);
        }

        public TestTable addEmptyColumns(int count) {
            columns().addAll(IntStream.range(0, count)
                .mapToObj(_ -> new EmptyColumn(StringUtils.randAlphabetic(6).toUpperCase()))
                .toList());
            return this;
        }

        static <E> TestCell<E> factory(User user, Function<User, E> extractor) {
            return factory(user, extractor, _ -> {});
        }

        static <E> TestCell<E> factory(User user, Function<User, E> extractor, Consumer<TestCell<E>> config) {
            Function<User, TestCell<E>> f = u -> new TestCell<>(u, extractor);
            TestCell<E> cell = f.apply(user);
            config.accept(cell);
            return cell;
        }
    }

    static class TestCell<E> extends VFXSimpleTableCell<User, E> {
        private Animation dbgAnim;

        public TestCell(User item, Function<User, E> extractor) {
            super(item, extractor);
        }

        @Override
        public void updateItem(User item) {
            User old = getItem();
            super.updateItem(item);

            //if (old != null) debugUpdate(!Objects.equals(old, item));
        }

        void debugUpdate(boolean success) {
            if (Animations.isPlaying(dbgAnim))
                dbgAnim.stop();

            Color start = Color.TRANSPARENT;
            Color end = success ? Color.rgb(0, 255, 0, 0.2) : Color.rgb(255, 0, 0, 0.2);
            dbgAnim = ConsumerTransition.of(
                f -> {
                    Color at = start.interpolate(end, f);
                    setBackground(Background.fill(at));
                },
                500, Interpolator.LINEAR
            );
            dbgAnim.setAutoReverse(true);
            dbgAnim.setCycleCount(2);
            dbgAnim.play();
        }
    }

    static class EmptyColumn extends VFXSimpleTableColumn<User, VFXTableCell<User>> {
        public EmptyColumn(String text) {
            super(text, randomFAS());
            setCellFactory(EmptyCell::new);
        }
    }

    static class EmptyCell extends TestCell<String> {
        public EmptyCell(User item) {
            super(item, _ -> "");
            setExtractor(_ -> data());

            observe(() -> VFXContainerEvent.update(this), rowProperty(), columnProperty()).listen();
        }

        String data() {
            String column = Optional.ofNullable(getColumn()).map(VFXTableColumn::getText).orElse("null");
            int row = getRowIndex();
            return "Column %s and Row %d".formatted(column, row);
        }
    }
}
