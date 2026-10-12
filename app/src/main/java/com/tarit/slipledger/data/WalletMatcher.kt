package com.tarit.slipledger.data

/** Decides which of the user's wallets a slip or alert belongs to. */
object WalletMatcher {
    /**
     * 1. a wallet with the same last-4 account digits;
     * 2. otherwise the only wallet of that bank (one without its own digits if digits are known).
     */
    fun match(wallets: List<Wallet>, bank: String, hint: String): Wallet? {
        if (hint.isNotEmpty()) wallets.firstOrNull { it.accountHint == hint }?.let { return it }
        if (bank.isEmpty()) return null
        val sameBank = wallets.filter { it.kind != WalletKind.CASH && it.bank == bank }
        return if (hint.isNotEmpty()) sameBank.singleOrNull { it.accountHint.isEmpty() }
        else sameBank.firstOrNull()
    }
}
