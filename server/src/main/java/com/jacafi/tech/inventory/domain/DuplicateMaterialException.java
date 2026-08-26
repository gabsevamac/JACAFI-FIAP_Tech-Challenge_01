package com.jacafi.tech.inventory.domain;

/**
 * A material was registered under a name another active item already carries.
 *
 * <p>The rule is not decoration. Two rows for the same part are two stock balances for one shelf,
 * and the workshop then has no way to answer how many it has — which is the dor
 * "falhas no controle de peças" reappearing inside the system meant to end it.
 *
 * <p>Enforced in the application layer, where the repository is, so the failure carries domain
 * meaning. The partial unique index in the database is the second line of defence, for two
 * concurrent registrations that both pass that check.
 */
public class DuplicateMaterialException extends RuntimeException {

    public DuplicateMaterialException(String message) {
        super(message);
    }
}
