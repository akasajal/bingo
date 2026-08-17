package com.ishaan.bingo.domain.model

data class BingoBoard(
    val numbers: List<Int?> = List(25) { null }
) {
    fun isComplete(): Boolean = numbers.all { it != null }
    
    fun isValid(): Boolean {
        val nonNullNumbers = numbers.filterNotNull()
        return nonNullNumbers.size == 25 && 
               nonNullNumbers.toSet().size == 25 && 
               nonNullNumbers.all { it in 1..25 }
    }
}
