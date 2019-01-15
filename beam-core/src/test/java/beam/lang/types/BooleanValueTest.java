package beam.lang.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

class BooleanValueTest {

    @Test
    void getValue() {
        assertThat(new BooleanValue("beam").getValue(), equalTo(false));
        assertThat(new BooleanValue("true").getValue(), equalTo(true));
        assertThat(new BooleanValue("false").getValue(), equalTo(false));

        // Negative assertions
        assertThat(new BooleanValue("beam").getValue(), not(true));
        assertThat(new BooleanValue("true").getValue(), not(false));
    }

    @Test
    void copy() {
        BooleanValue value = new BooleanValue("false");

        assertThat(value.copy(), not(value));
    }

    @Test
    void serialize() {
        assertThat(new BooleanValue("true").serialize(0), equalTo("true"));
        assertThat(new BooleanValue("true").serialize(4), equalTo("true"));
    }

}