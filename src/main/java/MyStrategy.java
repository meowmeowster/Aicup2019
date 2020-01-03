import model.*;

public class MyStrategy {
	static double distanceSqr(Vec2Double a, Vec2Double b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	public UnitAction getAction(Unit unit, Game game, Debug debug) {

		UnitAction action = new UnitAction();

		Unit nearestEnemy = null;
		for (Unit other : game.getUnits()) {
			if (other.getPlayerId() != unit.getPlayerId() && goodaim(game, unit, other, action)) {
				if (nearestEnemy == null || distanceSqr(unit.getPosition(),
						other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
					nearestEnemy = other;
				}
			}
		}
		LootBox nearestWeapon = null;
		LootBox nearestHealthpack = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.Weapon) {
				if (nearestWeapon == null || distanceSqr(unit.getPosition(),
						lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
					nearestWeapon = lootBox;
				}
			}
			if (lootBox.getItem() instanceof Item.HealthPack) {
				if (nearestHealthpack == null || distanceSqr(unit.getPosition(),
						lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealthpack.getPosition())) {
					nearestHealthpack = lootBox;
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


		//debug.draw(new CustomData.Log("Target pos: " + targetPos));
		Vec2Double aim = new Vec2Double(0, 0);
		if (nearestEnemy != null) {
			aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
					nearestEnemy.getPosition().getY() - unit.getPosition().getY());
			//debug.draw(new CustomData.Log("aim X: " + aim.getX()));
			//debug.draw(new CustomData.Log("aim Y: " + aim.getY()));
		}

		if 	(unit.getWeapon() != null && nearestHealthpack != null &&
				(
					(nearestEnemy == null) ||
					(unit.getHealth() < 100 && !unit.getWeapon().getTyp().equals(WeaponType.ASSAULT_RIFLE)) ||
					(unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER) && goodaim(game, unit, nearestEnemy, action)) ||
					(unit.getHealth() <= nearestEnemy.getHealth() && unit.getWeapon().getTyp().equals(WeaponType.ASSAULT_RIFLE)))
				)
		{
			targetPos = nearestHealthpack.getPosition();
		}

		boolean jump = (targetPos.getY() > unit.getPosition().getY());
		if (targetPos.getX() > unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
			//if (nearestEnemy != null && game.getLevel()
			//		.getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY()-10)] == Tile.WALL)
			//{

			//}
		}
		if (targetPos.getX() < unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
			//if (nearestEnemy != null && game.getLevel()
			//		.getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY()-10)] == Tile.WALL)
			//{

			//}
		}

		if (bazookashotrule(unit, aim, game, nearestEnemy, action)) {
			if (nearestEnemy == null || (gunshotrule(unit, aim))) {
				action.setVelocity((targetPos.getX() - unit.getPosition().getX()) * 100);
			} else {
				action.setVelocity((unit.getPosition().getX() - nearestEnemy.getPosition().getX()) * 100);
			}
		} else {
			action.setVelocity((targetPos.getX() - unit.getPosition().getX()));
		}


		action.setJump(jump);
		action.setJumpDown(!jump);

		action.setAim(aim);
		//if (nearestEnemy != null && avoidwalls(game, unit) && bazookashotrule(unit, nearestEnemy)) {
		if (nearestEnemy != null && avoidwalls(game, unit, aim) && goodaim(game, unit, nearestEnemy, action)) {
			action.setShoot(true);
		} else {
			action.setShoot(false);
		}
		action.setReload(false);
		action.setSwapWeapon(false);
		action.setPlantMine(false);
		return action;
	}

	public boolean bazookashotrule(Unit unit, Vec2Double aim, Game game, Unit nearestEnemy, UnitAction action) {
		if (!(unit.getWeapon() == null)) {
			if (unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER) && goodaim(game, unit, nearestEnemy, action)) {
				return false;
			}
		}
		return true;
	}

	public boolean gunshotrule(Unit unit, Vec2Double aim) {
		if (!(unit.getWeapon() == null)) {
			if (Math.sqrt((Math.pow(aim.getX(), 2) + Math.pow(aim.getY(), 2))) < 3) {
				return true;
			}
		}
		return true;
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
		//if (Math.sqrt((Math.pow(aim.getX(), 2) + Math.pow(aim.getY(), 2))) < 1 && unit.getWeapon()!=null) {
//			if (!unit.getWeapon().getTyp().equals(WeaponType.ROCKET_LAUNCHER)){
//				return false;
//			}
		//	action.setJump(true);
		//return true;
		//}
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
		//возвращает 0, если аргумент (x) равен нулю; -1, если x < 0 и 1, если x > 0.
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
						action.setJump(true);
						//return false;
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

}

