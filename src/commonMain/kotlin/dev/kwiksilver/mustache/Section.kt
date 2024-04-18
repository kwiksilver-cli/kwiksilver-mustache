package dev.kwiksilver.mustache

fun List<Fragment>.constructSections(): List<Fragment> {
    val updatedFragments = mutableListOf<Fragment>()
    val sectionStack = mutableListOf<SectionBuilder>()

    var currentFragmentCollector = updatedFragments

    for (fragment in this) {
        when (fragment) {
            is SectionStartFragment -> {
                val newSection = SectionBuilder(fragment.valuePath, fragment.position)
                sectionStack.add(newSection)
                currentFragmentCollector = newSection.fragments
            }
            is InvertedSectionStartFragment -> {
                val newSection = SectionBuilder(fragment.valuePath, fragment.position, inverted = true)
                sectionStack.add(newSection)
                currentFragmentCollector = newSection.fragments
            }
            is SectionEndFragment -> {
                val endSectionValuePath = fragment.valuePath
                if (sectionStack.isEmpty() || sectionStack.last().valuePath != endSectionValuePath) {
                    if (sectionStack.isNotEmpty()) {
                        val lastSection = sectionStack.last()
                        updatedFragments.add(ErrorFragment("Unclosed section ${lastSection.valuePath.joinToString(separator = ".")}", lastSection.position))
                    }
                    updatedFragments.add(ErrorFragment("Section close ${endSectionValuePath.joinToString(separator = ".")} without matching section open", fragment.position))
                }

                val sectionBuilder = sectionStack.removeLast()
                if (sectionStack.isEmpty()) {
                    currentFragmentCollector = updatedFragments
                } else {
                    currentFragmentCollector = sectionStack.last().fragments
                }

                currentFragmentCollector.add(sectionBuilder.build())
            }
            else -> currentFragmentCollector.add(fragment)
        }
    }

    return updatedFragments
}

private class SectionBuilder(val valuePath: ValuePath, val position: Int, val inverted: Boolean = false) {
    val fragments = mutableListOf<Fragment>()

    fun build(): Fragment = if (!inverted) {
        SectionFragment(valuePath, fragments, position)
    } else {
        InvertedSectionFragment(valuePath, fragments, position)
    }
}