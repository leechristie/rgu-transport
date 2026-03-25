/*
 * Copyright © 2021-2026 Robert Gordon University
 *
 * This library is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this library. If
 * not, see <https://www.gnu.org/licenses/>.
 */

package rgu.transport.geospatial.osm;

import java.util.*;

/**
 * Represents a relation member in an OSM relation.
 *
 * @author Lee A. Christie
 */
public final class RelationMember {

    private long reference;
    private String role;

    private RelationMember(long reference, String role) {
        this.reference = reference;
        this.role = role;
    }

    /**
     * Creates a new relation member.
     *
     * @param reference the reference attribute
     * @param role the role attribute, not null
     * @return the new relation member
     */
    public static RelationMember of(long reference, String role) {
        Objects.requireNonNull(role, "role");
        return new RelationMember(reference, role);
    }

    /**
     * Returns the reference attribute.
     *
     * @return the reference attribute
     */
    public long reference() {
        return this.reference;
    }

    /**
     * Returns the role attribute.
     *
     * @return the role attribute, not null
     */
    public String role() {
        return this.role;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RelationMember)) {
            return false;
        }
        RelationMember other = (RelationMember) o;
        return this.reference == other.reference
                && this.role.equals(other.role);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return 7127 * Long.hashCode(this.reference)
                + 54679 * this.role.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.reference + ":" + this.role;
    }

}
