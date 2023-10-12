package com.xwray.groupie;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SectionTest {

    @Mock
    GroupAdapter groupAdapter;

    private final int footerSize = 5;
    private Group footer = mock(Group.class);

    private final int headerSize = 2;
    private Group header = mock(Group.class);

    private final int placeholderSize = 3;
    private Group placeholder = mock(Group.class);

    private Group emptyGroup = mock(Group.class);

    @Before
    public void setUp() {
        when(header.getItemCount()).thenReturn(headerSize);
        when(footer.getItemCount()).thenReturn(footerSize);
        when(placeholder.getItemCount()).thenReturn(placeholderSize);
        when(emptyGroup.getItemCount()).thenReturn(0);
    }

    @Test
    public void settingFooterNotifiesFooterAddedAndRegistersItToGroupDataObserver() {
        Section section = new Section();
        section.setHeader(header);
        section.add(new DummyItem());
        section.registerGroupDataObserver(groupAdapter);
        section.setFooter(footer);

        verify(footer).registerGroupDataObserver(section);
        verify(groupAdapter).onItemRangeInserted(section, headerSize + 1, footerSize);
    }

    @Test
    public void settingNewFooterUnregistersOldFooterFromGroupDataObserver() {
        Group oldFooter = mock(Group.class);

        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setHeader(oldFooter);
        section.setHeader(footer);

        verify(oldFooter).unregisterGroupDataObserver(section);
        verify(footer).registerGroupDataObserver(section);
    }

    @Test
    public void removingFooterNotifiesPreviousFooterRemovedAndUnregistersItFromGroupDataObserver() {
        Section section = new Section();
        section.setHeader(header);
        section.add(new DummyItem());
        section.setFooter(footer);
        section.registerGroupDataObserver(groupAdapter);
        section.removeFooter();

        verify(footer).unregisterGroupDataObserver(section);
        verify(groupAdapter).onItemRangeRemoved(section, headerSize + 1, footerSize);
    }

    @Test(expected = NullPointerException.class)
    public void settingNullFooterThrowsNullPointerException() {
        Section section = new Section();
        section.setFooter(null);
    }

    @Test
    public void footerCountIs0WhenThereIsNoFooter() {
        Section section = new Section();
        section.removeFooter();

        assertEquals(0, section.getItemCount());
    }

    @Test
    public void footerCountIsSizeOfFooter() {
        Section section = new Section();

        section.setFooter(footer);
        assertEquals(footerSize, section.getItemCount());
    }

    @Test
    public void settingHeaderNotifiesHeaderAddedAndRegistersItToGroupDataObserver() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setHeader(header);

        verify(header).registerGroupDataObserver(section);
        verify(groupAdapter).onItemRangeInserted(section, 0, headerSize);
    }

    @Test
    public void settingNewHeaderUnregistersOldHeaderFromGroupDataObserver() {
        Group oldHeader = mock(Group.class);

        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setHeader(oldHeader);
        section.setHeader(header);

        verify(oldHeader).unregisterGroupDataObserver(section);
        verify(header).registerGroupDataObserver(section);
    }

    @Test
    public void removingHeaderNotifiesPreviousHeaderRemovedAndUnregistersItFromGroupDataObserver() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setHeader(header);
        section.removeHeader();

        verify(header).unregisterGroupDataObserver(section);
        verify(groupAdapter).onItemRangeRemoved(section, 0, headerSize);
    }

    @Test(expected = NullPointerException.class)
    public void settingNullHeaderThrowsNullPointerException() {
        Section section = new Section();
        section.setFooter(null);
    }

    @Test
    public void headerCountIs0WhenThereIsNoHeader() {
        Section section = new Section();
        section.removeHeader();

        assertEquals(0, section.getItemCount());
    }

    @Test
    public void headerCountIsSizeOfHeader() {
        Section section = new Section();

        section.setHeader(header);
        assertEquals(headerSize, section.getItemCount());
    }

    @Test
    public void getGroup() {
        Section section = new Section();
        Item item = new DummyItem();
        section.add(item);
        assertEquals(0, section.getPosition(item));
    }

    @Test
    public void getPositionReturnsNegativeIfItemNotPresent() {
        Section section = new Section();
        Item item = new DummyItem();
        assertEquals(-1, section.getPosition(item));
    }

    @Test
    public void constructorSetsListenerOnChildrenAndHeader() {
        List<Group> children = new ArrayList<>();
        Item item = mock(Item.class);
        children.add(item);
        Section section = new Section(header, children);

        verify(header).registerGroupDataObserver(section);
        verify(item).registerGroupDataObserver(section);
    }

    @Test
    public void setPlaceholderOnEmptySectionAddsPlaceholder() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.registerGroupDataObserver(groupAdapter);
        section.setPlaceholder(placeholder);

        verify(groupAdapter).onItemRangeInserted(section, headerSize, placeholderSize);
    }

    @Test
    public void getGroupReturnsPlaceholder() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setPlaceholder(placeholder);

        assertEquals(placeholder, section.getGroup(1));
    }

    @Test
    public void setPlaceholderOnNonEmptySectionDoesNotAddPlaceholder() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.add(new DummyItem());
        section.registerGroupDataObserver(groupAdapter);
        section.setPlaceholder(placeholder);

        verify(groupAdapter, never()).onItemRangeInserted(any(Section.class), anyInt(), anyInt());
    }

    @Test
    public void placeholderIsIncludedInItemCountIfBodyIsEmpty() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setPlaceholder(placeholder);

        assertEquals(headerSize + placeholderSize + footerSize, section.getItemCount());
    }

    @Test
    public void placeholderIsNotIncludedInItemCountIfBodyHasContent() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setPlaceholder(placeholder);
        section.add(new DummyItem());

        assertEquals(headerSize + footerSize + 1, section.getItemCount());
    }

    @Test
    public void addEmptyBodyContentDoesNotRemovePlaceholder() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setPlaceholder(placeholder);
        section.add(emptyGroup);

        verify(groupAdapter, never()).onItemRangeRemoved(any(Section.class), anyInt(), anyInt());
    }

    @Test
    public void addBodyContentRemovesPlaceholder() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setPlaceholder(placeholder);
        section.add(new DummyItem());

        verify(groupAdapter).onItemRangeRemoved(section, 0, placeholderSize);
    }

    @Test
    public void removeAllBodyContentAddsPlaceholder() {
        Section section = new Section();
        section.setPlaceholder(placeholder);
        Item item = new DummyItem();
        section.add(item);
        section.registerGroupDataObserver(groupAdapter);
        section.remove(item);

        verify(groupAdapter).onItemRangeInserted(section, 0, placeholderSize);
    }

    @Test
    public void removeAllBodyContentByModifyingAChildGroupAddsPlaceholder() {
        Section section = new Section();
        section.setPlaceholder(placeholder);
        Section childGroup = new Section();
        Item childItem = new DummyItem();
        childGroup.add(childItem);
        section.add(childGroup);
        section.registerGroupDataObserver(groupAdapter);
        childGroup.remove(childItem);

        verify(groupAdapter).onItemRangeInserted(section, 0, placeholderSize);
    }

    @Test
    public void removePlaceholderNotifies() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setPlaceholder(placeholder);
        section.registerGroupDataObserver(groupAdapter);
        section.removePlaceholder();

        verify(groupAdapter).onItemRangeRemoved(section, headerSize, placeholderSize);
    }

    @Test
    public void setHideWhenEmptyRemovesAnExistingPlaceholder() {
        Section section = new Section();
        section.setPlaceholder(placeholder);
        section.registerGroupDataObserver(groupAdapter);
        section.setHideWhenEmpty(true);

        verify(groupAdapter).onItemRangeRemoved(section, 0, placeholderSize);
    }

    @Test
    public void replacingAnExistingPlaceholderNotifiesChange() {
        Section section = new Section();
        section.setPlaceholder(placeholder);
        section.registerGroupDataObserver(groupAdapter);

        final int newPlaceholderSize = 1;
        Group newPlaceholder = new DummyGroup() {
            @Override
            public int getItemCount() {
                return newPlaceholderSize;
            }
        };
        section.setPlaceholder(newPlaceholder);

        verify(groupAdapter).onItemRangeRemoved(section, 0, placeholderSize);
        verify(groupAdapter).onItemRangeInserted(section, 0, newPlaceholderSize);
    }

    @Test
    public void setHeaderAddsHeader() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setHeader(header);

        verify(groupAdapter).onItemRangeInserted(section, 0, headerSize);
    }

    @Test
    public void removeHeaderRemovesHeader() {
        Section section = new Section();
        section.setHeader(header);
        section.registerGroupDataObserver(groupAdapter);
        section.removeHeader();

        verify(groupAdapter).onItemRangeRemoved(section, 0, headerSize);
    }

    @Test
    public void setFooterAddsFooter() {
        Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.setFooter(footer);

        verify(groupAdapter).onItemRangeInserted(section, 0, footerSize);
    }

    @Test
    public void removeFooterRemovesFooter() {
        Section section = new Section();
        section.setFooter(footer);
        section.registerGroupDataObserver(groupAdapter);
        section.removeFooter();

        verify(groupAdapter).onItemRangeRemoved(section, 0, footerSize);
    }

    @Test
    public void setHideWhenEmptyRemovesExistingHeaderAndFooter() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.registerGroupDataObserver(groupAdapter);
        section.setHideWhenEmpty(true);

        verify(groupAdapter).onItemRangeRemoved(section, 0, headerSize + footerSize);
    }

    @Test
    public void setHideWhenEmptyRemovesExistingHeaderFooterAndPlaceholder() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setPlaceholder(placeholder);
        section.registerGroupDataObserver(groupAdapter);
        section.setHideWhenEmpty(true);

        verify(groupAdapter).onItemRangeRemoved(section, 0, headerSize + footerSize + placeholderSize);
    }

    @Test
    public void setHideWhenEmptyFalseAddsExistingHeaderAndFooter() {
        Section section = new Section();
        section.setHeader(header);
        section.setFooter(footer);
        section.setHideWhenEmpty(true);
        section.registerGroupDataObserver(groupAdapter);
        section.setHideWhenEmpty(false);

        verify(groupAdapter).onItemRangeInserted(section, 0, headerSize);
        verify(groupAdapter).onItemRangeInserted(section, headerSize, footerSize);
    }

    @Test
    public void itemCountIsZeroWhenSetHideWhenEmptyTrue() {
        Section section = new Section();
        section.setHeader(header);
        section.setPlaceholder(placeholder);
        section.setFooter(footer);
        section.setHideWhenEmpty(true);

        assertEquals(0, section.getItemCount());
    }

    @Test
    public void groupCountIsHeaderFooterAndChildrenWhenNonEmpty() {
        Section section = new Section();
        section.setHeader(header);
        section.setPlaceholder(placeholder);
        section.setFooter(footer);
        section.add(new DummyItem());
        section.add(new DummyItem());

        assertEquals(4, section.getGroupCount());
    }

    @Test
    public void groupCountIsHeaderFooterAndPlaceholderWhenEmpty() {
        Section section = new Section();
        section.setHeader(header);
        section.setPlaceholder(placeholder);
        section.setFooter(footer);

        assertEquals(3, section.getGroupCount());
    }

    @Test
    public void groupCountIsZeroWhenEmptyAndSetHideWhenEmpty() {
        Section section = new Section();
        section.setHeader(header);
        section.setPlaceholder(placeholder);
        section.setFooter(footer);
        section.setHideWhenEmpty(true);

        assertEquals(0, section.getGroupCount());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenSectionIsEmptyAndSetHideWhenEmptyGetGroupThrowsException() {
        Section section = new Section();
        section.setHeader(header);
        section.setPlaceholder(placeholder);
        section.setFooter(footer);
        section.setHideWhenEmpty(true);
        section.getGroup(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addAllAtNonZeroPositionWhenEmptyThrowIndexOutOfBoundsException() {
        final Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);
        section.addAll(1, Arrays.asList(new DummyItem(), new DummyItem()));
    }

    @Test
    public void addAllAtPositionWhenEmptyNotifiesAdapterAtIndexZero() {
        final Section section = new Section();
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(0, Arrays.asList(new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 0, 2);
    }

    @Test
    public void addAllAtPositionWhenNonEmptyNotifiesAdapterAtCorrectIndex() {
        final Section section = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(2, Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 2, 3);
    }

    @Test
    public void addAllAtPositionWithEmptyNestedGroupNotifiesAdapterAtZeroIndex() {
        final Section nestedSection = new Section();

        final Section section = new Section();
        section.add(nestedSection);
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(1, Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 0, 3);
    }

    @Test
    public void addAllAtPositionFrontWithNestedGroupNotifiesAdapterAtCorrectIndex() {
        final List<DummyItem> nestedItems = Arrays.asList(new DummyItem(), new DummyItem());
        final Section nestedSection = new Section(nestedItems);

        final Section section = new Section();
        section.add(nestedSection);
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(0, Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 0, 3);
    }

    @Test
    public void addAllAtPositionMiddleWithNestedGroupNotifiesAdapterAtCorrectIndex() {
        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));

        final Section section = new Section(Arrays.asList(nestedSection1, nestedSection2));
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(1, Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 2, 3);
    }

    @Test
    public void addAllAtPositionEndWithNestedGroupNotifiesAdapterAtCorrectIndex() {
        final List<DummyItem> nestedItems = Arrays.asList(new DummyItem(), new DummyItem());
        final Section nestedSection = new Section(nestedItems);

        final Section section = new Section();
        section.add(nestedSection);
        section.registerGroupDataObserver(groupAdapter);

        section.addAll(1, Arrays.asList(new DummyItem(), new DummyItem()));
        verify(groupAdapter).onItemRangeInserted(section, 2, 2);
    }

    @Test
    public void addItemToNestedSectionNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section();
        rootSection.add(nestedSection2);

        reset(groupAdapter);
        nestedSection2.add(new DummyItem());
        verify(groupAdapter).onItemRangeInserted(rootSection, 3, 1);
    }

    @Test
    public void addGroupToNestedSectionNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));

        reset(groupAdapter);
        rootSection.add(nestedSection2);
        verify(groupAdapter).onItemRangeInserted(rootSection, 3, 2);
    }

    @Test
    public void addGroupToNestedSectionWithHeaderNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();
        rootSection.setHeader(new DummyItem());

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));

        reset(groupAdapter);
        rootSection.add(nestedSection2);
        verify(groupAdapter).onItemRangeInserted(rootSection, 4, 2);
    }

    @Test
    public void insertGroupToNestedSectionNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection2);

        final Section nestedSection3 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));

        reset(groupAdapter);
        rootSection.add(1, nestedSection3);
        verify(groupAdapter).onItemRangeInserted(rootSection, 2, 2);
    }

    @Test
    public void addAllWorksWithSets() {
        final Section testSection = new Section();

        Set<Item> itemSet = new HashSet<>();
        itemSet.add(new DummyItem());
        itemSet.add(new DummyItem());

        testSection.addAll(itemSet);
        assertEquals(2, testSection.getItemCount());
    }

    @Test
    public void removeGroupFromNestedSectionNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection2);

        reset(groupAdapter);
        rootSection.remove(nestedSection2);
        verify(groupAdapter).onItemRangeRemoved(rootSection, 3, 2);
    }

    @Test
    public void removeAllGroupFromNestedSectionNotifiesAtCorrectIndex() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection2);

        reset(groupAdapter);
        rootSection.removeAll(Collections.singletonList(nestedSection2));
        verify(groupAdapter).onItemRangeRemoved(rootSection, 3, 2);
    }

    @Test
    public void removeAllUnorderedGroupsFromNestedSectionNotifiesAtCorrectIndexes() {
        final Section rootSection = new Section();

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Collections.singletonList(new DummyItem()));
        rootSection.add(nestedSection2);

        final Section nestedSection3 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection3);

        reset(groupAdapter);
        rootSection.removeAll(Arrays.asList(nestedSection2, nestedSection3, nestedSection1));

        final InOrder adapterCalls = inOrder(groupAdapter, groupAdapter, groupAdapter);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 3, 1);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 3, 2);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 0, 3);
    }

    @Test
    public void clearRemovesAllBodyContents() {
        final Section rootSection = new Section();
        rootSection.setHeader(header);
        rootSection.setFooter(footer);

        rootSection.registerGroupDataObserver(groupAdapter);
        groupAdapter.add(rootSection);

        final Section nestedSection1 = new Section(Arrays.asList(new DummyItem(), new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection1);

        final Section nestedSection2 = new Section(Collections.singletonList(new DummyItem()));
        rootSection.add(nestedSection2);

        final Section nestedSection3 = new Section(Arrays.asList(new DummyItem(), new DummyItem()));
        rootSection.add(nestedSection3);

        reset(groupAdapter);
        rootSection.clear();

        final InOrder adapterCalls = inOrder(groupAdapter, groupAdapter, groupAdapter);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 2, 3);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 2, 1);
        adapterCalls.verify(groupAdapter).onItemRangeRemoved(rootSection, 2, 2);

        assertEquals(rootSection.getItemCount(), headerSize + footerSize);
    }

    @Test
    public void updateGroupChangesRange() {
        List<Item> children = new ArrayList<>();
        children.add(new AlwaysUpdatingItem(1));
        children.add(new AlwaysUpdatingItem(2));

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.registerGroupDataObserver(groupAdapter);

        group.update(children);
        verify(groupAdapter).onItemRangeInserted(group, 1, 2);
        verifyNoMoreInteractions(groupAdapter);

        group.update(children);
        verify(groupAdapter).onItemRangeChanged(group, 1, 2, null);
        verifyNoMoreInteractions(groupAdapter);
    }

    @Test
    public void notifyChangeInAnItemCausesParentToNotifyChange() {
        List<Item> children = new ArrayList<>();
        Item item = new DummyItem();
        children.add(item);

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.update(children);
        group.registerGroupDataObserver(groupAdapter);

        item.notifyChanged();

        verify(groupAdapter).onItemChanged(group, 1);
    }

    @Test
    public void updateWithTheSameItemAndSameContentsDoesNotNotifyChange() {
        List<Item> children = new ArrayList<>();
        Item item = new ContentUpdatingItem(1, "contents");
        children.add(item);

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.update(children);
        group.registerGroupDataObserver(groupAdapter);

        group.update(children);

        verifyNoMoreInteractions(groupAdapter);
    }

    @Test
    public void updateWithTheSameItemButDifferentContentsNotifiesChange() {
        Item oldItem = new ContentUpdatingItem(1, "contents");

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.update(Collections.singletonList(oldItem));
        group.registerGroupDataObserver(groupAdapter);

        Item newItem = new ContentUpdatingItem(1, "new contents");
        group.update(Collections.singletonList(newItem));

        verify(groupAdapter).onItemRangeChanged(group, 1, 1, null);
    }

    @Test
    public void updateWithADifferentItemNotifiesRemoveAndAdd() {
        Item oldItem = new ContentUpdatingItem(1, "contents");

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.update(Collections.singletonList(oldItem));
        group.registerGroupDataObserver(groupAdapter);

        Item newItem = new ContentUpdatingItem(2, "contents");
        group.update(Collections.singletonList(newItem));

        verify(groupAdapter).onItemRangeRemoved(group, 1, 1);
        verify(groupAdapter).onItemRangeInserted(group, 1, 1);
    }

    @Test
    public void updateWithANestedGroupsNotifiesRemoveAndAdd() {
        Item oldItem = new ContentUpdatingItem(1, "contents");

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.update(Collections.singletonList(oldItem));
        group.registerGroupDataObserver(groupAdapter);

        Item newItem = new ContentUpdatingItem(2, "new contents");
        Section newGroup = new Section();
        newGroup.add(newItem);
        group.update(Collections.singletonList(newGroup));

        verify(groupAdapter).onItemRangeRemoved(group, 1, 1);
        verify(groupAdapter).onItemRangeInserted(group, 1, 1);
    }

    @Test
    public void updateGroupWithPlaceholderNotifiesRemovePlaceholderAndInsert() {
        List<Item> children = new ArrayList<>();
        children.add(new AlwaysUpdatingItem(1));
        children.add(new AlwaysUpdatingItem(2));

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.setPlaceholder(new DummyItem());
        group.registerGroupDataObserver(groupAdapter);

        group.update(children);
        verify(groupAdapter).onItemRangeRemoved(group, 1, 1);
        verify(groupAdapter).onItemRangeInserted(group, 1, 2);
        verifyNoMoreInteractions(groupAdapter);
    }

    @Test
    public void updateGroupToEmptyWithPlaceholderNotifiesRemoveAndInsertPlaceholder() {
        List<Item> children = new ArrayList<>();
        children.add(new AlwaysUpdatingItem(1));
        children.add(new AlwaysUpdatingItem(2));

        Section group = new Section();
        group.setHeader(new DummyItem());
        group.setPlaceholder(new DummyItem());
        group.update(children);
        group.registerGroupDataObserver(groupAdapter);

        group.update(new ArrayList<Group>());

        verify(groupAdapter).onItemRangeRemoved(group, 1, 2);
        verify(groupAdapter).onItemRangeInserted(group, 1, 1);
        verifyNoMoreInteractions(groupAdapter);
    }
}
