/*
 * Copyright (C) 2024 Alexander Stojanovich <coas91@rocketmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package rs.alexanderstojanovich.evg.weapons;

import rs.alexanderstojanovich.evg.audio.AudioFile;
import rs.alexanderstojanovich.evg.critter.Critter;
import rs.alexanderstojanovich.evg.models.Model;

/**
 * Demolition Synergy weapon interface. Contains all texture, models and sounds
 * associated with weapon.
 *
 * @author Aleksandar Stojanovic <coas91@rocketmail.com>
 */
public class Weapon implements WeaponIfc {

    /*
        01 - M9 Pistol                - "W01M9.obj"
        02 - M1911 Pistol             - "W02M1.obj"
        03 - Desert Eagle             - "W03DE.obj"
        04 - Mini Uzi SMG             - "W04UZ.obj"
        05 - MP5 SMG                  - "W05M5.obj"
        06 - P90 SMG                  - "W06P9.obj"
        07 - AK47 Rifle               - "W07AK.obj"
        08 - M4A1 Rifle               - "W08M4.obj"
        09 - G36 Rifle                - "W09G3.obj"
        10 - M60 MG                   - "W10M6.obj"
        11 - SAW MG                   - "W11MS.obj"
        12 - Winchester 1200 Shotgun  - "W12W2.obj"
        13 - Benelli Super 90 Shotgun - "W13B9.obj"
        14 - Remington 700 Sniper     - "W14R7.obj"
        15 - Dragunov Sniper          - "W15DR.obj"
        16 - M82 Sniper               - "W16M8.obj"
     */
    /**
     * Base weapon model
     */
    public final Model model;
    /**
     * Weapon model on character first-person-shooter 'in hands'.
     */
    public final Model inHands;

    public final Clazz clazz;

    /**
     * Create new weapon from base model and weapon clazz.
     *
     * @param clazz weapon clazz (does not change)
     * @param baseModel base model (to create weapon from)
     */
    protected Weapon(Clazz clazz, Model baseModel) {
        this.clazz = clazz;
        this.model = baseModel;
        this.inHands = new Model(baseModel);
        this.inHands.pos = WeaponIfc.WEAPON_POS;
        this.inHands.setScale(2.71f);
        this.inHands.setrY((float) (-Math.PI / 2.0f));
    }

    /**
     * Derive model for character (or critter)
     *
     * @param critter critter having weapon
     * @return weapon on body for that critter
     */
    @Override
    public Model deriveBodyModel(Critter critter) {
        Model result = new Model(model);
        switch (clazz) {
            case None:
            default:
                break;
            case OneHandedSmallGun:
                result.pos.set(critter.body.pos.x + critter.body.getWidth() / 2.0f, critter.body.pos.y, 1.5f * (critter.body.pos.z + critter.body.getDepth() / 2.0f));
                break;
            case TwoHandedSmallGun:
                result.pos.set(critter.body.pos.x - critter.body.getWidth() / 2.0f, critter.body.pos.y, critter.body.pos.z + critter.body.getDepth() / 2.0f);
                break;
            case TwoHandedBigGuns:
                result.pos.set(critter.body.pos.x - critter.body.getWidth() / 2.0f, 1.5f * critter.body.pos.y, critter.body.pos.z + critter.body.getDepth() / 2.0f);
                break;
        }

        result.setScale(0.38f);
        result.setrY(critter.body.getrY());

        return result;
    }

    /**
     * Derive model on ground (as level container item).
     *
     * @return
     */
    @Override
    public Model deriveOnGroundItem() {
        Model result = new Model(model);

        return result;
    }

    public Model getInHands() {
        return inHands;
    }

    @Override
    public String getTexName() {
        return model.texName;
    }

    @Override
    public Model getModel() {
        return model;
    }

    @Override
    public Model inHands() {
        return inHands;
    }

    @Override
    public AudioFile getFireSoundFX() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Clazz getClazz() {
        return clazz;
    }

}
