package br.com.tokens.state

import br.com.tokens.model.Account
import br.com.tokens.schema.AccountSchemaV1
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class AccountState(
        private val account: Account,
        override val linearId: UniqueIdentifier = UniqueIdentifier()
) : MultipleOwnersState<Account>(account, account.participants.toList()), QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is AccountSchemaV1 -> AccountSchemaV1.PersistentAccount(
                    account.userMetadata.nationalId,
                    account.primaryFor?.name.toString())
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AccountSchemaV1)
}
