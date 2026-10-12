package com.tarit.slipledger.ui

import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxType

/** Navigation and global actions the screens can trigger, implemented once at the root. */
class AppActions(
    val scan: () -> Unit,
    val newTx: (TxType) -> Unit,
    val edit: (Tx) -> Unit,
    val showHistory: () -> Unit,
    /** Opens the transaction list filtered to one category. */
    val showCategory: (String) -> Unit,
    val showStats: () -> Unit,
    /** Opens the monthly budget editor. */
    val editBudget: () -> Unit,
    val openWallets: () -> Unit,
    /** Opens the transaction list filtered to one wallet. */
    val showWallet: (Long) -> Unit,
    val openReview: () -> Unit,
)
