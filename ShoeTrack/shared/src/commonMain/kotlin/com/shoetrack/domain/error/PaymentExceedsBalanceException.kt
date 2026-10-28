package com.shoetrack.domain.error

import com.shoetrack.domain.model.Money

class PaymentExceedsBalanceException(balance: Money) :
    IllegalArgumentException("The payment is more than the remaining balance of ${balance.cents} cents")
