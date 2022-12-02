/*
 * Copyright (C) 2022 Parisi Alessandro
 * This file is part of VirtualizedFX (https://github.com/palexdev/VirtualizedFX).
 *
 * VirtualizedFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VirtualizedFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with VirtualizedFX.  If not, see <http://www.gnu.org/licenses/>.
 */

package interactive.cells;

import io.github.palexdev.materialfx.factories.CornerRadiusFactory;
import io.github.palexdev.materialfx.factories.InsetsFactory;
import io.github.palexdev.mfxcore.animations.Animations;
import io.github.palexdev.mfxcore.animations.ConsumerTransition;
import io.github.palexdev.mfxcore.animations.Interpolators;
import io.github.palexdev.mfxcore.utils.RandomUtils;
import io.github.palexdev.mfxcore.utils.fx.StyleUtils;
import javafx.animation.Animation;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class CellsDebugger {
	private static final Map<Object, Color> dColors = new HashMap<>();

	private CellsDebugger() {
	}

	public static Color getColor(Integer item) {
		return dColors.computeIfAbsent(item, i -> Color.color(
				RandomUtils.random.nextFloat(),
				RandomUtils.random.nextFloat(),
				RandomUtils.random.nextFloat(),
				0.5f
		));
	}

	public static Color getColor(Integer item, double opacity) {
		return dColors.computeIfAbsent(item, i -> Color.color(
				RandomUtils.random.nextFloat(),
				RandomUtils.random.nextFloat(),
				RandomUtils.random.nextFloat(),
				opacity
		));
	}

	public static void clear() {
		dColors.clear();
	}

	public static void randBackground(Region r, double opacity, Integer item) {
		StyleUtils.setBackground(r, getColor(item, opacity));
	}

	public static void randBackground(Region r, double opacity, Integer item, double radius, double inset) {
		StyleUtils.setBackground(r, getColor(item, opacity), CornerRadiusFactory.all(radius), InsetsFactory.all(inset));
	}

	public static Animation animateBackground(Region region, double animationDuration) {
		int r = RandomUtils.random.nextInt(0, 255);
		int g = RandomUtils.random.nextInt(0, 255);
		int b = RandomUtils.random.nextInt(0, 255);
		ConsumerTransition ct1 = ConsumerTransition.of(frac -> StyleUtils.setBackground(region, Color.rgb(r, g, b, frac)))
				.setDuration(animationDuration)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		ConsumerTransition ct2 = ConsumerTransition.of(frac -> StyleUtils.setBackground(region, Color.rgb(r, g, b, 1.0 - frac)))
				.setDuration(500)
				.setInterpolatorFluent(Interpolators.INTERPOLATOR_V1);
		return Animations.SequentialBuilder.build()
				.add(ct1)
				.add(ct2)
				.getAnimation();
	}
}
