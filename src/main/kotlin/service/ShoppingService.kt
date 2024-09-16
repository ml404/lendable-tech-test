package service

import offers.Offer

interface ShoppingService {

    fun generateReceipt(): String

    fun addOffer(offer: Offer)
}