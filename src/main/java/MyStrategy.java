
import model.*;

public class MyStrategy {
	static double distanceSqr(Vec2Double a, Vec2Double b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	public UnitAction getAction(Unit unit, Game game, Debug debug) {

		UnitAction action = new UnitAction();

		Unit nearestEnemy = null;
		Unit closeEnemy = null;
		Unit friend = null;
		for (Unit other : game.getUnits()) {
			if (other.getPlayerId() == unit.getPlayerId() && !other.equals(unit)) {
				friend = other;
			}
			if (other.getPlayerId() != unit.getPlayerId() && goodaim(game, unit, other, action)) {
				if (nearestEnemy == null || distanceSqr(unit.getPosition(),
						other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
					nearestEnemy = other;
				}
			}
			if (other.getPlayerId() != unit.getPlayerId()) {
				if (closeEnemy == null || distanceSqr(unit.getPosition(),
						other.getPosition()) < distanceSqr(unit.getPosition(), closeEnemy.getPosition())) {
					closeEnemy = other;
				}
			}
		}
		LootBox nearestWeapon = null;
		LootBox nearestHealthpack = null;
		LootBox nearestMine = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.Weapon) {
				if (nearestWeapon == null
						|| (distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition()))
						|| (easyToGrab(unit, lootBox, game) && !easyToGrab(unit, nearestWeapon, game) && distanceSqr(unit.getPosition(), lootBox.getPosition()) <= 1.5*distanceSqr(unit.getPosition(), nearestWeapon.getPosition())
				)
				) {
					nearestWeapon = lootBox;
				}
			}
			if (lootBox.getItem() instanceof Item.HealthPack) {
				if (nearestHealthpack == null
						|| (distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealthpack.getPosition()))
						|| (easyToGrab(unit, lootBox, game) && !easyToGrab(unit, nearestHealthpack, game) && distanceSqr(unit.getPosition(), lootBox.getPosition()) <= 1.5*distanceSqr(unit.getPosition(), nearestHealthpack.getPosition())
				)
				) {
					nearestHealthpack = lootBox;
				}
			}
			if (lootBox.getItem() instanceof Item.Mine) {
				if (nearestMine == null
						|| (distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestMine.getPosition()))
						|| (easyToGrab(unit, lootBox, game) && !easyToGrab(unit, nearestMine, game) && distanceSqr(unit.getPosition(), lootBox.getPosition()) <= 1.5*distanceSqr(unit.getPosition(), nearestMine.getPosition())
				)
				) {
					nearestMine = lootBox;
				}
			}
		}
		Vec2Double targetPos = unit.getPosition();


		if (unit.getWeapon() == null && nearestWeapon != null) {
			targetPos = nearestWeapon.getPosition();
		} else if (nearestEnemy != null) {
			targetPos = nearestEnemy.getPosition();
		}

		if (targetPos.getY() < unit.getPosition().getY() - 10) {

			Vec2Double newpos = drawBresenhamLineSimple(
					(int) unit.getPosition().getX(), (int) unit.getPosition().getY(),
					((int) unit.getPosition().getX() - 10 * sign((int) unit.getPosition().getX() - (int) targetPos.getX())), (int) unit.getPosition().getY(), 1, game);
			if (newpos != null) {
				targetPos = newpos;
			}
		}

		boolean jump = (targetPos.getY() > unit.getPosition().getY() && Math.sin((double) game.getCurrentTick() / 3) != 0);

		Vec2Double aim = new Vec2Double(0, 0);
		if (nearestEnemy != null) {
			aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
					nearestEnemy.getPosition().getY() - unit.getPosition().getY() + aimCorrection(nearestEnemy));

		}

		if (unit.getWeapon() != null && nearestHealthpack != null &&
				(
						(nearestEnemy == null) ||
								(unit.getHealth() < 100 && !unit.getWeapon().getTyp().equals(WeaponType.ASSAULT_RIFLE)) ||
								(unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER) && goodaim(game, unit, nearestEnemy, action)) ||
								(unit.getHealth() < 100 && unit.getHealth() <= nearestEnemy.getHealth() * 1.2 && unit.getWeapon().getTyp().equals(WeaponType.ASSAULT_RIFLE)))
		) {
			targetPos = nearestHealthpack.getPosition();
			if (nearestEnemy == null && friend != null && distanceSqr(unit.getPosition(), friend.getPosition()) < Math.pow(game.getProperties().getMineExplosionParams().getRadius(),3)) {
				jump = true;
			}
		}

		if (unit.getWeapon() == null && nearestEnemy != null && nearestMine != null && nearestWeapon != null) {
			if (distanceSqr(unit.getPosition(), nearestMine.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition()) / 2 ||
					(easyToGrab(unit, nearestMine, game) && easyToGo(nearestMine.getPosition(), nearestWeapon.getPosition(), game))) {
				targetPos = nearestMine.getPosition();
			}
		}




		if (unit.getWeapon() == null && nearestHealthpack != null && unit.getHealth() < 90) {
			targetPos = nearestHealthpack.getPosition();
			if (nearestWeapon != null && easyToGrab(unit, nearestWeapon, game) && unit.getHealth() > 50) {
				targetPos = nearestWeapon.getPosition();
			}
		}


		LootBox nearestItem = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if ((nearestItem == null && (easyToGrab(unit, lootBox, game))
					|| ((nearestItem != null && distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestItem.getPosition())
					&& (distanceSqr(unit.getPosition(), lootBox.getPosition()) > 1))
					&& (easyToGrab(unit, lootBox, game) && !easyToGrab(unit, nearestItem, game))))) {
				nearestItem = lootBox;
			}

			if (lootBox.getItem() instanceof Item.Weapon) {
				if ((nearestWeapon == null
						|| (distanceSqr(unit.getPosition(), lootBox.getPosition()) < Math.pow(game.getProperties().getMineExplosionParams().getRadius(),3)))
						&& unit.getWeapon() == null
				) {
					targetPos = lootBox.getPosition();
				}
			}
		}
		double unitprops = unit.getMines() * 41 + (double) unit.getHealth() / 2;
		if (unit.getWeapon() != null) {
			if (unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)) {
				unitprops = unitprops + 80;
			} else if (unit.getWeapon().getTyp().equals(WeaponType.PISTOL)) {
				unitprops = unitprops - 133;
			}
		} else {
			unitprops = unitprops + 275;
		}

		Player alien = new Player();
		Player enemy = new Player();
		for (Player player : game.getPlayers()) {
			if (player.getId() == unit.getPlayerId()) {
				alien = player;
			} else {
				enemy = player;
			}
		}

		if (alien.getScore() <= enemy.getScore()) {
			if (game.getCurrentTick() >= (900 - (unitprops / 1.3)) && nearestItem != null && nearestEnemy == null) {
				targetPos = nearestItem.getPosition();
			}
			if (game.getCurrentTick() >= (1500 - (unitprops)) && (game.getCurrentTick() < 3600 && !(unit.getWeapon() != null && nearestEnemy != null))) {
				Vec2Double pos = new Vec2Double();
				try {
					double posX = unit.getPosition().getX() + (Math.sin((double) (game.getCurrentTick() + unitprops) / (33 + (double) game.getCurrentTick() / 253))) * (18 + (double) game.getCurrentTick() / 270);
					if (posX < 0) {
						posX = 0;
					} else if (posX > 39) {
						posX = 39;
					}
					pos.setX(posX);
					try {
						double posY = unit.getPosition().getY() + (Math.cos((double) (game.getCurrentTick() + unitprops) / (27 + (double) game.getCurrentTick() / 168))) * (12 + (double) game.getCurrentTick() / 270);
						if (posY < 0) {
							posY = 0;
						} else if (posY > 29) {
							posY = 29;
						}
						pos.setY(posY);
					} catch (Exception e) {
						pos.setY(unit.getPosition().getY());
					}
					targetPos = pos;
				} catch (Exception e) {
					pos.setX(unit.getPosition().getY());
					pos.setY(unit.getPosition().getX());
					targetPos = pos;
				}
			}
		}
		for (Mine mine : game.getMines()) {
			if (distanceSqr(unit.getPosition(), mine.getPosition()) < Math.pow(game.getProperties().getMineExplosionParams().getRadius(),3)) {
				jump = true;
				for (LootBox lootBox : game.getLootBoxes()) {
					if (distanceSqr(mine.getPosition(), lootBox.getPosition()) >= Math.pow(game.getProperties().getMineExplosionParams().getRadius(),3) && easyToGo(mine.getPosition(), lootBox.getPosition(), game)) {
						targetPos = lootBox.getPosition();
					}
				}
			}
		}

		if ((targetPos.getX() > unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) ||
				(nearestEnemy != null && friend != null &&
						unit.getPosition().getX() > friend.getPosition().getX() && nearestEnemy.getPosition().getX() > unit.getPosition().getX())
		) {
			jump = true;

		}
		if ((targetPos.getX() < unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) ||
				(nearestEnemy != null && friend != null &&
						unit.getPosition().getX() < friend.getPosition().getX() && nearestEnemy.getPosition().getX() < unit.getPosition().getX())
		) {
			jump = true;

		}

		try {
			if (unit.getWeapon() == null && (friend == null || friend.getWeapon() == null) && (game.getCurrentTick() < 500 && !goodaim(game, unit, nearestEnemy, action))) {
				Unit fr1;
				Unit fr2 = new Unit();
				Unit en1;
				Unit en2 = new Unit();

				if (game.getUnits()[0].getPlayerId() == unit.getPlayerId()) {
					fr1 = game.getUnits()[0];
					en1 = game.getUnits()[1];
				} else {
					fr1 = game.getUnits()[1];
					en1 = game.getUnits()[0];
				}
				if (fr1.getId() == unit.getId()) {
					targetPos.setX(40 - en1.getPosition().getX());
					targetPos.setY(en1.getPosition().getY());
				}
				if (game.getUnits().length > 2) {
					if (game.getUnits()[2].getPlayerId() == unit.getPlayerId()) {
						fr2 = game.getUnits()[2];
						en2 = game.getUnits()[3];
					} else {
						fr2 = game.getUnits()[3];
						en2 = game.getUnits()[2];
					}
					if (fr2.getId() == unit.getId()) {
						targetPos.setX(40 - en2.getPosition().getX());
						targetPos.setY(en2.getPosition().getY());
					}
				}

				if (fr1.getPosition().getX() > 40-en1.getPosition().getX()) {
					action.setVelocity(-100);
				} else {
					action.setVelocity(100);
				}
				if (fr1.getPosition().getY() > en1.getPosition().getY()) {
					action.setJumpDown(true);
					action.setJump(false);
				} else {
					action.setJumpDown(false);
					action.setJump(true);
				}
				if (game.getUnits().length > 2) {
					if (fr2.getPosition().getX() > 40-en2.getPosition().getX()) {
						action.setVelocity(-100);
					} else {
						action.setVelocity(100);
					}
					if (fr2.getPosition().getY() > en2.getPosition().getY()) {
						action.setJumpDown(true);
						action.setJump(false);
					} else {
						action.setJumpDown(false);
						action.setJump(true);
					}
				}
			}
		} catch (Exception e) {
		}

		if (nearestEnemy!=null && easyToGo(unit.getPosition(), nearestEnemy.getPosition(), game) &&
				unit.getWeapon()!= null && (unit.getMines() > 1 || (unit.getMines() > 0 && unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)))) {
			targetPos = nearestEnemy.getPosition();
		}

		action.setVelocity(Double.compare(targetPos.getX() - unit.getPosition().getX(), 0) * 100);


		if (nearestEnemy != null && avoidwalls(game, unit, aim) && goodaim(game, unit, nearestEnemy, action)) {
			action.setShoot(true);
		} else {
			action.setShoot(false);
		}

		action.setPlantMine(false);

		if (unit.getMines() > 0 && nearestEnemy != null &&
				distanceSqr(unit.getPosition(), nearestEnemy.getPosition()) < Math.pow(game.getProperties().getMineExplosionParams().getRadius(),2)) {
			jump = false;
			action.setPlantMine(true);
		}

		action.setJump(jump);
		action.setJumpDown(!jump);

		action.setAim(aim);

		try {
			if (game.getMines().length > 0 && unit.getWeapon() != null
					&& closeEnemy != null && distanceSqr(game.getMines()[0].getPosition(), closeEnemy.getPosition()) < Math.pow(game.getProperties().getMineExplosionParams().getRadius(),3)
					&& easyToGo(unit.getPosition(), game.getMines()[0].getPosition(), game)) {
				aim.setX(game.getMines()[0].getPosition().getX() - unit.getPosition().getX());
				aim.setY(game.getMines()[0].getPosition().getY() - unit.getPosition().getY() - 1);
				action.setAim(aim);
				action.setShoot(true);
			}
		} catch (Exception e) {
			// do nothing
		}

		action.setReload(goodreload(unit, nearestEnemy, game));
		action.setSwapWeapon((unit.getWeapon() != null && !unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)));


		return action;
	}

	public boolean avoidwalls(Game game, Unit unit, Vec2Double aim) {
		int x = (int) unit.getPosition().getX();
		int y = (int) unit.getPosition().getY();
		Tile t1 = game.getLevel().getTiles()[x + sign((int) aim.getX())][y];

		if (!(unit.getWeapon() == null)) {
			if (unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER) &&
					(t1 == Tile.WALL)) {
				return false;
			}
		}
		return true;
	}

	public boolean goodaim(Game game, Unit unit, Unit nearestEnemy, UnitAction action) {
		try {
			return drawBresenhamLine((int) unit.getPosition().getX(), (int) unit.getPosition().getY(),
					(int) nearestEnemy.getPosition().getX(), (int) nearestEnemy.getPosition().getY(),
					game, unit, action);
		} catch (Exception e) {
			return true;
		}
	}

	private int sign(int x) {
		return Integer.compare(x, 0);
	}

	public boolean drawBresenhamLine(int xstart, int ystart, int xend, int yend, Game game, Unit unit, UnitAction action)
	/**
	 * xstart, ystart - начало;
	 * xend, yend - конец;
	 * "g.drawLine (x, y, x, y);" используем в качестве "setPixel (x, y);"
	 * Можно писать что-нибудь вроде g.fillRect (x, y, 1, 1);
	 */
	{
		int x, y, dx, dy, incx, incy, pdx, pdy, es, el, err;

		dx = xend - xstart;//проекция на ось икс
		dy = yend - ystart;//проекция на ось игрек

		incx = sign(dx);
		/*
		 * Определяем, в какую сторону нужно будет сдвигаться. Если dx < 0, т.е. отрезок идёт
		 * справа налево по иксу, то incx будет равен -1.
		 * Это будет использоваться в цикле постороения.
		 */
		incy = sign(dy);
		/*
		 * Аналогично. Если рисуем отрезок снизу вверх -
		 * это будет отрицательный сдвиг для y (иначе - положительный).
		 */

		if (dx < 0) dx = -dx;//далее мы будем сравнивать: "if (dx < dy)"
		if (dy < 0) dy = -dy;//поэтому необходимо сделать dx = |dx|; dy = |dy|
		//эти две строчки можно записать и так: dx = Math.abs(dx); dy = Math.abs(dy);

		if (dx > dy)
		//определяем наклон отрезка:
		{
			/*
			 * Если dx > dy, то значит отрезок "вытянут" вдоль оси икс, т.е. он скорее длинный, чем высокий.
			 * Значит в цикле нужно будет идти по икс (строчка el = dx;), значит "протягивать" прямую по иксу
			 * надо в соответствии с тем, слева направо и справа налево она идёт (pdx = incx;), при этом
			 * по y сдвиг такой отсутствует.
			 */
			pdx = incx;
			pdy = 0;
			es = dy;
			el = dx;
		} else//случай, когда прямая скорее "высокая", чем длинная, т.е. вытянута по оси y
		{
			pdx = 0;
			pdy = incy;
			es = dx;
			el = dy;//тогда в цикле будем двигаться по y
		}

		x = xstart;
		y = ystart;
		err = el / 2;
		//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!g.drawLine (x, y, x, y);//ставим первую точку
		if (game.getLevel().getTiles()[x][y] == Tile.WALL) {
			return false;
		}
		for (Unit other : game.getUnits()) {
			if (other.getPlayerId() == unit.getPlayerId() && !other.equals(unit)) {
				//if (Math.abs((int) other.getPosition().getX() - x) <= 2 && Math.abs((int) other.getPosition().getY() - y) <= 2 &&
				//		(sign((int) nearestEnemy.getPosition().getX() - x) == sign((int) other.getPosition().getX() - x) ||
				//				sign((int) nearestEnemy.getPosition().getY() - y) == sign((int) other.getPosition().getY() - y)))  {
				//	action.setJump(true);
				//	return false;
				//	}
				if ((int) other.getPosition().getX() == x && (int) other.getPosition().getY() == y) {
					action.setJump(true);
					//return false;
				}
			}
		}
		//все последующие точки возможно надо сдвигать, поэтому первую ставим вне цикла

		for (int t = 0; t < el; t++)//идём по всем точкам, начиная со второй и до последней
		{
			err -= es;
			if (err < 0) {
				err += el;
				x += incx;//сдвинуть прямую (сместить вверх или вниз, если цикл проходит по иксам)
				y += incy;//или сместить влево-вправо, если цикл проходит по y
			} else {
				x += pdx;//продолжить тянуть прямую дальше, т.е. сдвинуть влево или вправо, если
				y += pdy;//цикл идёт по иксу; сдвинуть вверх или вниз, если по y
			}
			if (game.getLevel().getTiles()[x][y] == Tile.WALL) {
				if (unit.getWeapon() != null && unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER) && Math.abs(xend - x) < 2 && Math.abs(yend - y) < 2
						&& Math.sqrt(Math.pow(xstart - x, 2) + Math.pow(ystart - y, 2)) > unit.getWeapon().getParams().getExplosion().getRadius()) {
					// do nothing
				} else {
					return false;
				}
			}
			for (Unit other : game.getUnits()) {
				if (other.getPlayerId() == unit.getPlayerId() && !other.equals(unit)) {
					//if (Math.abs((int) other.getPosition().getX() - x) <= 2 && Math.abs((int) other.getPosition().getY() - y) <= 2 &&
					//		(sign((int) nearestEnemy.getPosition().getX() - x) == sign((int) other.getPosition().getX() - x) ||
					//				sign((int) nearestEnemy.getPosition().getY() - y) == sign((int) other.getPosition().getY() - y)))  {
					//	action.setJump(true);
					//	return false;
					//	}
					if ((int) other.getPosition().getX() == x && (int) other.getPosition().getY() == y) {
						//action.setJump(true);
						return false;
					}
				}
			}
			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!g.drawLine (x, y, x, y);
		}
		return true;
	}


	public Vec2Double drawBresenhamLineSimple(int xstart, int ystart, int xend, int yend, int condition, Game game)
	/**
	 * xstart, ystart - начало;
	 * xend, yend - конец;
	 * "g.drawLine (x, y, x, y);" используем в качестве "setPixel (x, y);"
	 * Можно писать что-нибудь вроде g.fillRect (x, y, 1, 1);
	 */
	{
		int x, y, dx, dy, incx, incy, pdx, pdy, es, el, err;

		dx = xend - xstart;//проекция на ось икс
		dy = yend - ystart;//проекция на ось игрек

		incx = sign(dx);
		/*
		 * Определяем, в какую сторону нужно будет сдвигаться. Если dx < 0, т.е. отрезок идёт
		 * справа налево по иксу, то incx будет равен -1.
		 * Это будет использоваться в цикле постороения.
		 */
		incy = sign(dy);
		/*
		 * Аналогично. Если рисуем отрезок снизу вверх -
		 * это будет отрицательный сдвиг для y (иначе - положительный).
		 */

		if (dx < 0) dx = -dx;//далее мы будем сравнивать: "if (dx < dy)"
		if (dy < 0) dy = -dy;//поэтому необходимо сделать dx = |dx|; dy = |dy|
		//эти две строчки можно записать и так: dx = Math.abs(dx); dy = Math.abs(dy);

		if (dx > dy)
		//определяем наклон отрезка:
		{
			/*
			 * Если dx > dy, то значит отрезок "вытянут" вдоль оси икс, т.е. он скорее длинный, чем высокий.
			 * Значит в цикле нужно будет идти по икс (строчка el = dx;), значит "протягивать" прямую по иксу
			 * надо в соответствии с тем, слева направо и справа налево она идёт (pdx = incx;), при этом
			 * по y сдвиг такой отсутствует.
			 */
			pdx = incx;
			pdy = 0;
			es = dy;
			el = dx;
		} else//случай, когда прямая скорее "высокая", чем длинная, т.е. вытянута по оси y
		{
			pdx = 0;
			pdy = incy;
			es = dx;
			el = dy;//тогда в цикле будем двигаться по y
		}

		x = xstart;
		y = ystart;
		err = el / 2;
		//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!g.drawLine (x, y, x, y);//ставим первую
		if (condition == 1) {
			if (game.getLevel().getTiles()[x][y] == Tile.LADDER || game.getLevel().getTiles()[x][y] == Tile.PLATFORM) {
				Vec2Double newpos = new Vec2Double();
				newpos.setX(x);
				newpos.setY(y);
				return newpos;
			}
		}

		//все последующие точки возможно надо сдвигать, поэтому первую ставим вне цикла

		for (int t = 0; t < el; t++)//идём по всем точкам, начиная со второй и до последней
		{
			err -= es;
			if (err < 0) {
				err += el;
				x += incx;//сдвинуть прямую (сместить вверх или вниз, если цикл проходит по иксам)
				y += incy;//или сместить влево-вправо, если цикл проходит по y
			} else {
				x += pdx;//продолжить тянуть прямую дальше, т.е. сдвинуть влево или вправо, если
				y += pdy;//цикл идёт по иксу; сдвинуть вверх или вниз, если по y
			}


			//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!g.drawLine (x, y, x, y);
		}
		return null;
	}

	public boolean goodplace(int x, int y, Game game) {
		try {
			if (game.getLevel().getTiles()[x][y] != Tile.WALL && game.getLevel().getTiles()[x][y - 1] != Tile.WALL) {
				return true;
			}
			return false;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean goodreload(Unit unit, Unit nearestEnemy, Game game) {
		if (unit.getWeapon() != null && !unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)
				&& unit.getWeapon().getParams().getMagazineSize() > unit.getWeapon().getMagazine()) {
			try {
				if (nearestEnemy == null) {
					return true;
				} else {
					return false;
				}
			} catch (Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	public boolean easyToGrab(Unit unit, LootBox lootBox, Game game) {
		return drawBL((int) unit.getPosition().getX(), (int) unit.getPosition().getY(), (int) lootBox.getPosition().getX(), (int) lootBox.getPosition().getY(), game);
	}

	public boolean easyToGo(Vec2Double pos1, Vec2Double pos2, Game game) {
		return drawBL((int) pos1.getX(), (int) pos1.getY(), (int) pos2.getX(), (int) pos2.getY(), game);
	}

	public boolean drawBL(int xstart, int ystart, int xend, int yend, Game game) {
		try {
			int x, y, dx, dy, incx, incy, pdx, pdy, es, el, err;
			dx = xend - xstart;
			dy = yend - ystart;
			incx = sign(dx);
			incy = sign(dy);
			if (dx < 0) dx = -dx;
			if (dy < 0) dy = -dy;
			if (dx > dy) {
				pdx = incx;
				pdy = 0;
				es = dy;
				el = dx;
			} else {
				pdx = 0;
				pdy = incy;
				es = dx;
				el = dy;
			}
			x = xstart;
			y = ystart;
			err = el / 2;
			if (game.getLevel().getTiles()[x][y] == Tile.WALL) {
				return false;
			}
			for (int t = 0; t < el; t++) {
				err -= es;
				if (err < 0) {
					err += el;
					x += incx;
					y += incy;
				} else {
					x += pdx;
					y += pdy;
				}
				if (game.getLevel().getTiles()[x][y] == Tile.WALL) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public double aimCorrection(Unit nearestEnemy) {
		if (nearestEnemy == null) {
			return 0;
		} else if (nearestEnemy.isOnGround()) {
			return 1;
		} else if (!nearestEnemy.getJumpState().isCanCancel()) {
			return Double.compare(nearestEnemy.getJumpState().getSpeed(),0);
		}
		return 0;
	}
}

