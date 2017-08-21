/*
 * Copyright 2017, Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opencensus.implcore.tags;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;
import io.opencensus.tags.Tag;
import io.opencensus.tags.Tag.TagBoolean;
import io.opencensus.tags.Tag.TagLong;
import io.opencensus.tags.Tag.TagString;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContexts;
import io.opencensus.tags.TagKey.TagKeyBoolean;
import io.opencensus.tags.TagKey.TagKeyLong;
import io.opencensus.tags.TagKey.TagKeyString;
import io.opencensus.tags.TagValueString;
import io.opencensus.tags.UnreleasedApiAccessor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link TagContextImpl} and {@link TagContextBuilderImpl}. */
// TODO(sebright): Add more tests once the API is finalized.
@RunWith(JUnit4.class)
public class TagContextImplTest {
  private final TagContexts tagContexts = new TagContextsImpl();

  private static final TagKeyString KS1 = TagKeyString.create("k1");
  private static final TagKeyString KS2 = TagKeyString.create("k2");

  private static final TagValueString V1 = TagValueString.create("v1");
  private static final TagValueString V2 = TagValueString.create("v2");

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void allowMutlipleKeysWithSameNameButDifferentTypes() {
    TagKeyString stringKey = TagKeyString.create("key");
    TagKeyLong longKey = UnreleasedApiAccessor.createTagKeyLong("key");
    TagKeyBoolean boolKey = UnreleasedApiAccessor.createTagKeyBoolean("key");
    assertThat(
            asList(
                tagContexts
                    .emptyBuilder()
                    .set(stringKey, TagValueString.create("value"))
                    .set(longKey, 123)
                    .set(boolKey, true)
                    .build()))
        .containsExactly(
            TagString.create(stringKey, TagValueString.create("value")),
            TagLong.create(longKey, 123L),
            TagBoolean.create(boolKey, true));
  }

  @Test
  public void testSet() {
    TagContext tags = tagContexts.emptyBuilder().set(KS1, V1).build();
    assertThat(asList(tagContexts.toBuilder(tags).set(KS1, V2).build()))
        .containsExactly(TagString.create(KS1, V2));
    assertThat(asList(tagContexts.toBuilder(tags).set(KS2, V2).build()))
        .containsExactly(TagString.create(KS1, V1), TagString.create(KS2, V2));
  }

  @Test
  public void testClear() {
    TagContext tags = tagContexts.emptyBuilder().set(KS1, V1).build();
    assertThat(asList(tagContexts.toBuilder(tags).clear(KS1).build())).isEmpty();
    assertThat(asList(tagContexts.toBuilder(tags).clear(KS2).build()))
        .containsExactly(TagString.create(KS1, V1));
  }

  @Test
  public void testIterator() {
    TagContext tags = tagContexts.emptyBuilder().set(KS1, V1).set(KS2, V2).build();
    Iterator<Tag> i = tags.unsafeGetIterator();
    assertTrue(i.hasNext());
    Tag tag1 = i.next();
    assertTrue(i.hasNext());
    Tag tag2 = i.next();
    assertFalse(i.hasNext());
    assertThat(Arrays.asList(tag1, tag2))
        .containsExactly(TagString.create(KS1, V1), TagString.create(KS2, V2));
    thrown.expect(NoSuchElementException.class);
    i.next();
  }

  @Test
  public void disallowCallingRemoveOnIterator() {
    TagContext tags = tagContexts.emptyBuilder().set(KS1, V1).set(KS2, V2).build();
    Iterator<Tag> i = tags.unsafeGetIterator();
    i.next();
    thrown.expect(UnsupportedOperationException.class);
    i.remove();
  }

  private static List<Tag> asList(TagContext tags) {
    return Lists.newArrayList(tags.unsafeGetIterator());
  }
}