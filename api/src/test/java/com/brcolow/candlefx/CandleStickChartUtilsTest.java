package com.brcolow.candlefxtest;

import com.brcolow.candlefx.CandleData;
import com.brcolow.candlefx.CandleStickChartUtils;
import com.brcolow.candlefx.Extrema;
import javafx.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * @author Michael Ennen
 */
public class CandleStickChartUtilsTest {
    @Test
    public void testExtrema() {
        List<CandleData> candleData = new ArrayList<>();
        candleData.add(new CandleData(1, 1.5, 2, 1, 0, 1));
        candleData.add(new CandleData(1.5, 2, 2, 1, 1, 5));
        candleData.add(new CandleData(2, 1, 2, 1, 2, 2));
        candleData.add(new CandleData(1, 3, 3, 1, 3, 8));
        candleData.add(new CandleData(1, 3, 4, 1, 4, 8));

        Map<Integer, Pair<Extrema<Integer>, Extrema<Integer>>> extrema = new HashMap<>();
        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 1);
        assertThat(extrema).containsExactly(entry(0, new Pair<>(new Extrema<>(1, 1), new Extrema<>(1, 2))),
                entry(1, new Pair<>(new Extrema<>(5, 5), new Extrema<>(1, 2))),
                entry(2, new Pair<>(new Extrema<>(2, 2), new Extrema<>(1, 2))),
                entry(3, new Pair<>(new Extrema<>(8, 8), new Extrema<>(1, 3))),
                entry(4, new Pair<>(new Extrema<>(8, 8), new Extrema<>(1, 4))));

        extrema.clear();

        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 2);
        assertThat(extrema).containsExactly(entry(0, new Pair<>(new Extrema<>(1, 5), new Extrema<>(1, 2))),
                entry(1, new Pair<>(new Extrema<>(2, 5), new Extrema<>(1, 2))),
                entry(2, new Pair<>(new Extrema<>(2, 8), new Extrema<>(1, 3))),
                entry(3, new Pair<>(new Extrema<>(8, 8), new Extrema<>(1, 4))));

        extrema.clear();

        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 3);
        assertThat(extrema).containsExactly(entry(0, new Pair<>(new Extrema<>(1, 5), new Extrema<>(1, 2))),
                entry(1, new Pair<>(new Extrema<>(2, 8), new Extrema<>(1, 3))),
                entry(2, new Pair<>(new Extrema<>(2, 8), new Extrema<>(1, 4))));

        extrema.clear();

        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 4);
        assertThat(extrema).containsExactly(entry(0, new Pair<>(new Extrema<>(1, 8), new Extrema<>(1, 3))),
                entry(1, new Pair<>(new Extrema<>(2, 8), new Extrema<>(1, 4))));

        candleData.add(new CandleData(3, 2, 5, 2, 5, 12));

        extrema.clear();
        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 2);
        assertThat(extrema).containsExactly(entry(0, new Pair<>(new Extrema<>(1, 5), new Extrema<>(1, 2))),
                entry(1, new Pair<>(new Extrema<>(2, 5), new Extrema<>(1, 2))),
                entry(2, new Pair<>(new Extrema<>(2, 8), new Extrema<>(1, 3))),
                entry(3, new Pair<>(new Extrema<>(8, 8), new Extrema<>(1, 4))),
                entry(4, new Pair<>(new Extrema<>(8, 12), new Extrema<>(1, 5))));
    }

    @Test
    public void test201CandlesFromCoinbase() {
        // given
        Map<Integer, Pair<Extrema<Integer>, Extrema<Integer>>> extrema = new HashMap<>();
        List<CandleData> candleData = new ArrayList<>();
        candleData.add(new CandleData(654.120000, 654.110000, 654.120000, 654.110000, 1468467300, 1.408404));
        candleData.add(new CandleData(654.110000, 655.190000, 655.190000, 654.080000, 1468467600, 13.826230));
        candleData.add(new CandleData(654.770000, 655.240000, 655.320000, 654.770000, 1468467900, 8.391380));
        candleData.add(new CandleData(655.420000, 655.800000, 656.000000, 655.300000, 1468468200, 26.057798));
        candleData.add(new CandleData(655.760000, 655.520000, 655.770000, 655.520000, 1468468500, 9.568800));
        candleData.add(new CandleData(655.510000, 655.310000, 655.510000, 654.750000, 1468468800, 15.851580));
        candleData.add(new CandleData(655.310000, 654.680000, 655.320000, 654.330000, 1468469100, 8.742489));
        candleData.add(new CandleData(654.710000, 654.090000, 654.790000, 654.090000, 1468469400, 8.026760));
        candleData.add(new CandleData(654.380000, 654.340000, 654.420000, 654.220000, 1468469700, 4.725120));
        candleData.add(new CandleData(654.220000, 654.140000, 654.350000, 654.060000, 1468470000, 7.698032));
        candleData.add(new CandleData(654.340000, 654.010000, 654.420000, 654.000000, 1468470300, 33.002919));
        candleData.add(new CandleData(654.010000, 654.010000, 654.010000, 654.000000, 1468470600, 7.835456));
        candleData.add(new CandleData(654.010000, 654.000000, 654.010000, 654.000000, 1468470900, 8.411360));
        candleData.add(new CandleData(654.000000, 654.000000, 654.010000, 654.000000, 1468471200, 5.244030));
        candleData.add(new CandleData(654.010000, 653.950000, 654.490000, 653.910000, 1468471500, 35.739038));
        candleData.add(new CandleData(653.880000, 653.510000, 653.880000, 653.500000, 1468471800, 9.788991));
        candleData.add(new CandleData(653.590000, 653.580000, 654.300000, 653.510000, 1468472100, 19.103830));
        candleData.add(new CandleData(653.570000, 654.770000, 654.880000, 653.570000, 1468472400, 3.545208));
        candleData.add(new CandleData(654.780000, 655.470000, 655.480000, 654.770000, 1468472700, 17.678490));
        candleData.add(new CandleData(654.990000, 655.030000, 655.440000, 654.530000, 1468473000, 7.165280));
        candleData.add(new CandleData(655.030000, 655.430000, 655.640000, 654.860000, 1468473300, 16.511790));
        candleData.add(new CandleData(655.430000, 655.830000, 655.840000, 655.430000, 1468473600, 20.522110));
        candleData.add(new CandleData(655.840000, 655.840000, 655.840000, 655.830000, 1468473900, 6.229850));
        candleData.add(new CandleData(655.840000, 655.840000, 655.840000, 655.820000, 1468474200, 7.879410));
        candleData.add(new CandleData(655.830000, 655.990000, 656.000000, 655.830000, 1468474500, 1.667380));
        candleData.add(new CandleData(655.960000, 656.160000, 656.160000, 655.960000, 1468474800, 9.210630));
        candleData.add(new CandleData(656.070000, 658.000000, 658.000000, 656.060000, 1468475100, 10.984233));
        candleData.add(new CandleData(658.020000, 657.280000, 658.020000, 657.280000, 1468475400, 3.804146));
        candleData.add(new CandleData(657.520000, 657.370000, 657.520000, 657.370000, 1468475700, 4.272180));
        candleData.add(new CandleData(657.370000, 657.500000, 657.680000, 657.360000, 1468476000, 3.249410));
        candleData.add(new CandleData(657.420000, 657.970000, 657.970000, 657.420000, 1468476300, 1.880540));
        candleData.add(new CandleData(657.800000, 657.640000, 657.820000, 657.640000, 1468476600, 2.130020));
        candleData.add(new CandleData(657.560000, 657.390000, 657.650000, 657.360000, 1468476900, 18.662457));
        candleData.add(new CandleData(657.410000, 656.310000, 657.410000, 656.310000, 1468477200, 23.045680));
        candleData.add(new CandleData(656.480000, 656.400000, 656.930000, 656.400000, 1468477500, 6.908320));
        candleData.add(new CandleData(656.640000, 656.740000, 657.350000, 656.640000, 1468477800, 3.268700));
        candleData.add(new CandleData(656.650000, 657.240000, 657.240000, 656.650000, 1468478100, 7.406680));
        candleData.add(new CandleData(657.160000, 656.790000, 657.160000, 656.360000, 1468478400, 2.463710));
        candleData.add(new CandleData(656.730000, 655.880000, 656.730000, 655.170000, 1468478700, 26.615079));
        candleData.add(new CandleData(655.780000, 655.890000, 655.890000, 655.040000, 1468479000, 4.424543));
        candleData.add(new CandleData(655.890000, 655.960000, 655.990000, 655.160000, 1468479300, 6.213140));
        candleData.add(new CandleData(655.950000, 656.190000, 656.500000, 655.950000, 1468479600, 12.785840));
        candleData.add(new CandleData(656.210000, 656.360000, 656.360000, 656.110000, 1468479900, 2.780911));
        candleData.add(new CandleData(656.350000, 656.500000, 656.500000, 656.200000, 1468480200, 3.370248));
        candleData.add(new CandleData(656.500000, 656.210000, 656.510000, 656.010000, 1468480500, 7.935810));
        candleData.add(new CandleData(656.210000, 656.200000, 656.500000, 656.200000, 1468480800, 19.164810));
        candleData.add(new CandleData(656.190000, 656.020000, 656.200000, 656.010000, 1468481100, 1.125080));
        candleData.add(new CandleData(656.020000, 656.030000, 656.190000, 656.020000, 1468481400, 12.740929));
        candleData.add(new CandleData(656.030000, 656.010000, 656.030000, 656.000000, 1468481700, 4.873650));
        candleData.add(new CandleData(656.010000, 656.010000, 656.010000, 656.000000, 1468482000, 2.922500));
        candleData.add(new CandleData(656.000000, 655.920000, 656.000000, 655.830000, 1468482300, 10.701690));
        candleData.add(new CandleData(655.920000, 656.200000, 656.350000, 655.920000, 1468482600, 8.500080));
        candleData.add(new CandleData(656.200000, 655.840000, 656.200000, 655.830000, 1468482900, 8.097610));
        candleData.add(new CandleData(655.840000, 655.230000, 655.840000, 655.230000, 1468483200, 2.889043));
        candleData.add(new CandleData(655.240000, 655.240000, 655.240000, 655.230000, 1468483500, 2.659410));
        candleData.add(new CandleData(655.240000, 655.260000, 655.390000, 655.230000, 1468483800, 1.390332));
        candleData.add(new CandleData(655.260000, 655.460000, 655.570000, 655.260000, 1468484100, 2.679250));
        candleData.add(new CandleData(655.460000, 655.440000, 655.460000, 655.370000, 1468484400, 0.852690));
        candleData.add(new CandleData(655.430000, 655.540000, 655.560000, 655.430000, 1468484700, 3.366580));
        candleData.add(new CandleData(655.550000, 655.540000, 655.590000, 655.470000, 1468485000, 7.546203));
        candleData.add(new CandleData(655.540000, 655.630000, 655.630000, 655.540000, 1468485300, 3.409720));
        candleData.add(new CandleData(655.620000, 655.810000, 655.810000, 655.620000, 1468485600, 3.846580));
        candleData.add(new CandleData(655.780000, 655.740000, 655.860000, 655.570000, 1468485900, 3.692150));
        candleData.add(new CandleData(655.830000, 655.380000, 655.830000, 655.290000, 1468486200, 1.399790));
        candleData.add(new CandleData(655.380000, 655.490000, 655.620000, 655.380000, 1468486500, 1.891810));
        candleData.add(new CandleData(655.500000, 655.580000, 655.580000, 655.490000, 1468486800, 0.514473));
        candleData.add(new CandleData(655.530000, 655.390000, 655.540000, 655.160000, 1468487100, 4.173360));
        candleData.add(new CandleData(655.310000, 655.510000, 655.550000, 655.310000, 1468487400, 2.608213));
        candleData.add(new CandleData(655.420000, 655.470000, 655.520000, 655.330000, 1468487700, 1.871650));
        candleData.add(new CandleData(655.470000, 655.700000, 655.700000, 655.450000, 1468488000, 8.563440));
        candleData.add(new CandleData(655.740000, 655.860000, 656.010000, 655.740000, 1468488300, 14.872310));
        candleData.add(new CandleData(655.830000, 655.980000, 655.980000, 655.820000, 1468488600, 2.227390));
        candleData.add(new CandleData(655.820000, 655.780000, 655.820000, 655.780000, 1468488900, 0.204050));
        candleData.add(new CandleData(655.780000, 655.680000, 655.780000, 655.600000, 1468489200, 1.681930));
        candleData.add(new CandleData(655.680000, 655.610000, 655.690000, 655.610000, 1468489500, 5.941130));
        candleData.add(new CandleData(655.600000, 655.440000, 655.610000, 655.440000, 1468489800, 3.022160));
        candleData.add(new CandleData(655.450000, 655.630000, 655.630000, 655.450000, 1468490100, 3.784810));
        candleData.add(new CandleData(655.630000, 656.180000, 656.180000, 655.630000, 1468490400, 0.710510));
        candleData.add(new CandleData(656.190000, 656.930000, 656.930000, 656.190000, 1468490700, 5.126490));
        candleData.add(new CandleData(656.930000, 657.660000, 657.660000, 656.930000, 1468491000, 4.364160));
        candleData.add(new CandleData(657.660000, 656.180000, 657.660000, 655.700000, 1468491300, 61.321026));
        candleData.add(new CandleData(656.180000, 656.180000, 656.190000, 656.180000, 1468491600, 0.397330));
        candleData.add(new CandleData(656.340000, 657.230000, 657.270000, 656.340000, 1468491900, 4.149190));
        candleData.add(new CandleData(657.220000, 657.450000, 657.500000, 657.190000, 1468492200, 8.286329));
        candleData.add(new CandleData(657.280000, 657.560000, 657.560000, 657.280000, 1468492500, 12.448590));
        candleData.add(new CandleData(657.570000, 657.690000, 657.690000, 657.570000, 1468492800, 5.919751));
        candleData.add(new CandleData(657.700000, 657.870000, 657.880000, 657.700000, 1468493100, 4.683540));
        candleData.add(new CandleData(657.860000, 657.960000, 657.960000, 657.860000, 1468493400, 1.924550));
        candleData.add(new CandleData(657.950000, 657.990000, 658.000000, 657.950000, 1468493700, 3.549550));
        candleData.add(new CandleData(657.990000, 658.110000, 658.110000, 657.990000, 1468494000, 2.578500));
        candleData.add(new CandleData(658.110000, 659.000000, 659.000000, 658.100000, 1468494300, 3.284947));
        candleData.add(new CandleData(658.970000, 659.640000, 659.640000, 658.930000, 1468494600, 10.823758));
        candleData.add(new CandleData(659.640000, 661.000000, 661.000000, 659.640000, 1468494900, 14.781936));
        candleData.add(new CandleData(661.000000, 661.680000, 661.700000, 660.990000, 1468495200, 27.352488));
        candleData.add(new CandleData(661.680000, 661.540000, 661.960000, 661.480000, 1468495500, 11.789266));
        candleData.add(new CandleData(661.540000, 661.370000, 661.540000, 661.370000, 1468495800, 9.058410));
        candleData.add(new CandleData(661.350000, 661.430000, 661.440000, 661.320000, 1468496100, 3.488770));
        candleData.add(new CandleData(661.430000, 661.040000, 661.430000, 661.040000, 1468496400, 5.155320));
        candleData.add(new CandleData(661.230000, 661.120000, 661.230000, 661.010000, 1468496700, 5.574657));
        candleData.add(new CandleData(661.120000, 660.950000, 661.120000, 660.950000, 1468497000, 6.429080));
        candleData.add(new CandleData(661.110000, 660.670000, 661.110000, 660.670000, 1468497300, 2.454040));
        candleData.add(new CandleData(660.690000, 660.950000, 660.960000, 660.540000, 1468497600, 4.550820));
        candleData.add(new CandleData(660.960000, 661.870000, 661.870000, 660.920000, 1468497900, 21.163976));
        candleData.add(new CandleData(661.300000, 660.890000, 661.440000, 660.540000, 1468498200, 11.161540));
        candleData.add(new CandleData(660.910000, 661.350000, 661.370000, 660.880000, 1468498500, 6.200090));
        candleData.add(new CandleData(661.140000, 661.290000, 661.350000, 660.990000, 1468498800, 15.408197));
        candleData.add(new CandleData(661.290000, 661.290000, 661.290000, 661.280000, 1468499100, 11.610230));
        candleData.add(new CandleData(661.350000, 661.990000, 661.990000, 661.280000, 1468499400, 44.913186));
        candleData.add(new CandleData(661.290000, 661.940000, 662.000000, 661.290000, 1468499700, 44.972480));
        candleData.add(new CandleData(661.940000, 662.000000, 662.000000, 661.450000, 1468500000, 21.077108));
        candleData.add(new CandleData(662.000000, 662.040000, 662.040000, 661.990000, 1468500300, 20.753148));
        candleData.add(new CandleData(662.040000, 662.000000, 662.340000, 661.800000, 1468500600, 69.497351));
        candleData.add(new CandleData(661.990000, 662.000000, 662.000000, 661.990000, 1468500900, 16.709690));
        candleData.add(new CandleData(662.000000, 662.290000, 662.300000, 662.000000, 1468501200, 22.043109));
        candleData.add(new CandleData(662.230000, 661.910000, 662.410000, 661.910000, 1468501500, 13.949340));
        candleData.add(new CandleData(662.100000, 662.420000, 662.430000, 662.100000, 1468501800, 3.873396));
        candleData.add(new CandleData(662.340000, 662.130000, 662.340000, 661.870000, 1468502100, 3.384052));
        candleData.add(new CandleData(661.940000, 662.050000, 662.430000, 660.770000, 1468502400, 48.657500));
        candleData.add(new CandleData(662.060000, 661.640000, 662.230000, 661.640000, 1468502700, 10.364960));
        candleData.add(new CandleData(661.640000, 662.000000, 662.000000, 661.490000, 1468503000, 23.803782));
        candleData.add(new CandleData(662.000000, 662.530000, 662.530000, 661.650000, 1468503300, 15.730310));
        candleData.add(new CandleData(662.530000, 662.470000, 662.650000, 662.230000, 1468503600, 11.636220));
        candleData.add(new CandleData(662.620000, 662.990000, 662.990000, 662.620000, 1468503900, 30.686710));
        candleData.add(new CandleData(662.980000, 662.860000, 662.980000, 662.460000, 1468504200, 9.186250));
        candleData.add(new CandleData(662.830000, 662.740000, 662.950000, 662.740000, 1468504500, 13.107630));
        candleData.add(new CandleData(662.740000, 662.800000, 662.950000, 662.740000, 1468504800, 6.263130));
        candleData.add(new CandleData(662.790000, 662.950000, 662.950000, 662.700000, 1468505100, 15.233040));
        candleData.add(new CandleData(662.870000, 662.990000, 662.990000, 662.460000, 1468505400, 23.147887));
        candleData.add(new CandleData(662.930000, 662.850000, 662.970000, 662.640000, 1468505700, 4.787190));
        candleData.add(new CandleData(662.850000, 662.670000, 662.990000, 662.360000, 1468506000, 9.044680));
        candleData.add(new CandleData(662.680000, 662.740000, 662.990000, 662.280000, 1468506300, 20.500810));
        candleData.add(new CandleData(662.740000, 662.970000, 662.990000, 662.710000, 1468506600, 21.564653));
        candleData.add(new CandleData(662.950000, 662.980000, 662.990000, 662.710000, 1468506900, 22.034891));
        candleData.add(new CandleData(662.960000, 662.880000, 662.990000, 662.820000, 1468507200, 15.936430));
        candleData.add(new CandleData(662.880000, 662.870000, 662.990000, 662.140000, 1468507500, 54.734880));
        candleData.add(new CandleData(662.830000, 662.900000, 663.000000, 662.370000, 1468507800, 22.464430));
        candleData.add(new CandleData(662.740000, 662.850000, 662.890000, 662.370000, 1468508100, 5.742310));
        candleData.add(new CandleData(662.850000, 662.330000, 662.850000, 662.270000, 1468508400, 15.046093));
        candleData.add(new CandleData(662.370000, 662.550000, 662.850000, 662.130000, 1468508700, 19.917857));
        candleData.add(new CandleData(662.380000, 662.720000, 662.820000, 662.000000, 1468509000, 12.210450));
        candleData.add(new CandleData(662.720000, 661.960000, 662.740000, 660.530000, 1468509300, 42.388043));
        candleData.add(new CandleData(661.920000, 662.170000, 662.170000, 661.270000, 1468509600, 30.668180));
        candleData.add(new CandleData(661.840000, 661.930000, 662.140000, 660.970000, 1468509900, 41.329022));
        candleData.add(new CandleData(661.800000, 661.960000, 662.260000, 661.800000, 1468510200, 2.664570));
        candleData.add(new CandleData(661.960000, 662.330000, 662.400000, 661.720000, 1468510500, 23.519020));
        candleData.add(new CandleData(662.320000, 662.380000, 662.380000, 661.720000, 1468510800, 11.313260));
        candleData.add(new CandleData(662.380000, 662.910000, 662.980000, 662.380000, 1468511100, 11.290641));
        candleData.add(new CandleData(662.920000, 662.910000, 662.980000, 662.340000, 1468511400, 17.544850));
        candleData.add(new CandleData(662.910000, 662.760000, 662.920000, 662.340000, 1468511700, 5.105560));
        candleData.add(new CandleData(662.750000, 662.520000, 662.960000, 662.470000, 1468512000, 23.767540));
        candleData.add(new CandleData(662.510000, 662.750000, 662.750000, 662.090000, 1468512300, 25.540070));
        candleData.add(new CandleData(662.310000, 661.900000, 662.950000, 661.900000, 1468512600, 12.388800));
        candleData.add(new CandleData(662.150000, 661.900000, 662.590000, 661.900000, 1468512900, 9.354050));
        candleData.add(new CandleData(662.300000, 662.470000, 662.600000, 662.070000, 1468513200, 12.349190));
        candleData.add(new CandleData(662.540000, 662.650000, 662.660000, 662.520000, 1468513500, 6.938120));
        candleData.add(new CandleData(662.660000, 662.340000, 662.690000, 662.340000, 1468513800, 8.787327));
        candleData.add(new CandleData(662.580000, 662.690000, 662.690000, 662.490000, 1468514100, 38.138815));
        candleData.add(new CandleData(662.690000, 662.540000, 662.690000, 662.370000, 1468514400, 8.746061));
        candleData.add(new CandleData(662.570000, 662.690000, 662.690000, 662.350000, 1468514700, 17.216840));
        candleData.add(new CandleData(662.450000, 662.980000, 662.980000, 662.410000, 1468515000, 28.666447));
        candleData.add(new CandleData(662.980000, 663.760000, 663.950000, 662.980000, 1468515300, 27.043560));
        candleData.add(new CandleData(663.940000, 663.970000, 663.990000, 663.710000, 1468515600, 9.427070));
        candleData.add(new CandleData(663.940000, 663.930000, 663.940000, 663.920000, 1468515900, 11.888920));
        candleData.add(new CandleData(663.920000, 663.100000, 663.930000, 663.100000, 1468516200, 8.026030));
        candleData.add(new CandleData(663.100000, 663.390000, 663.530000, 663.100000, 1468516500, 10.194520));
        candleData.add(new CandleData(663.410000, 663.980000, 663.990000, 663.380000, 1468516800, 16.735716));
        candleData.add(new CandleData(663.980000, 663.990000, 663.990000, 663.910000, 1468517100, 3.762600));
        candleData.add(new CandleData(663.980000, 663.890000, 663.990000, 663.530000, 1468517400, 23.300740));
        candleData.add(new CandleData(663.900000, 663.900000, 663.990000, 663.510000, 1468517700, 13.189202));
        candleData.add(new CandleData(663.900000, 663.920000, 663.990000, 663.610000, 1468518000, 24.402826));
        candleData.add(new CandleData(663.790000, 663.930000, 663.990000, 663.750000, 1468518300, 19.673854));
        candleData.add(new CandleData(663.970000, 663.990000, 663.990000, 663.970000, 1468518600, 15.927355));
        candleData.add(new CandleData(663.980000, 663.760000, 663.990000, 663.550000, 1468518900, 18.023380));
        candleData.add(new CandleData(663.820000, 663.850000, 663.880000, 663.540000, 1468519200, 9.160817));
        candleData.add(new CandleData(663.850000, 663.960000, 663.960000, 663.540000, 1468519500, 20.625793));
        candleData.add(new CandleData(663.950000, 663.940000, 663.980000, 663.590000, 1468519800, 32.567683));
        candleData.add(new CandleData(663.970000, 663.980000, 663.980000, 663.930000, 1468520100, 10.455513));
        candleData.add(new CandleData(663.970000, 663.980000, 663.990000, 663.920000, 1468520400, 8.353172));
        candleData.add(new CandleData(663.970000, 663.980000, 663.990000, 663.920000, 1468520700, 7.450997));
        candleData.add(new CandleData(663.980000, 663.990000, 663.990000, 663.920000, 1468521000, 12.154800));
        candleData.add(new CandleData(663.980000, 663.970000, 663.990000, 663.680000, 1468521300, 8.974810));
        candleData.add(new CandleData(663.980000, 663.990000, 663.990000, 663.950000, 1468521600, 9.139215));
        candleData.add(new CandleData(663.990000, 663.960000, 663.990000, 663.950000, 1468521900, 13.313442));
        candleData.add(new CandleData(663.970000, 663.880000, 663.990000, 663.420000, 1468522200, 62.768360));
        candleData.add(new CandleData(663.860000, 662.010000, 663.860000, 662.000000, 1468522500, 41.295908));
        candleData.add(new CandleData(662.000000, 662.000000, 662.180000, 662.000000, 1468522800, 15.223610));
        candleData.add(new CandleData(662.030000, 661.750000, 662.030000, 661.300000, 1468523100, 7.548373));
        candleData.add(new CandleData(661.560000, 661.230000, 661.560000, 661.100000, 1468523400, 8.745950));
        candleData.add(new CandleData(661.230000, 661.870000, 662.010000, 661.230000, 1468523700, 34.593049));
        candleData.add(new CandleData(661.760000, 662.000000, 662.000000, 661.700000, 1468524000, 20.492140));
        candleData.add(new CandleData(661.980000, 661.990000, 661.990000, 661.550000, 1468524300, 13.310416));
        candleData.add(new CandleData(661.930000, 660.790000, 662.010000, 659.640000, 1468524600, 73.957197));
        candleData.add(new CandleData(660.670000, 660.680000, 661.630000, 660.250000, 1468524900, 11.400780));
        candleData.add(new CandleData(660.650000, 661.250000, 661.250000, 660.540000, 1468525200, 7.802843));
        candleData.add(new CandleData(661.200000, 661.550000, 661.550000, 660.680000, 1468525500, 15.487623));
        candleData.add(new CandleData(661.550000, 661.390000, 661.960000, 661.130000, 1468525800, 5.791814));
        candleData.add(new CandleData(661.400000, 661.970000, 662.050000, 661.400000, 1468526100, 19.774715));
        candleData.add(new CandleData(661.710000, 661.990000, 662.230000, 661.500000, 1468526400, 41.689450));
        candleData.add(new CandleData(661.850000, 662.110000, 662.110000, 661.500000, 1468526700, 20.952457));
        candleData.add(new CandleData(662.090000, 662.150000, 662.250000, 662.000000, 1468527000, 6.993800));
        candleData.add(new CandleData(662.010000, 661.900000, 662.100000, 661.710000, 1468527300, 16.416019));

        // when
        CandleStickChartUtils.putSlidingWindowExtrema(extrema, candleData, 80);

        // then
        assertThat(extrema).containsOnly(entry(1468467300, new Pair<>(new Extrema<>(0, 36), new Extrema<>(653, 659))),
                entry(1468467600, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468467900, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468468200, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468468500, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468468800, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468469100, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468469400, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468469700, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468470000, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468470300, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468470600, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 659))),
                entry(1468470900, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 660))),
                entry(1468471200, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 661))),
                entry(1468471500, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 662))),
                entry(1468471800, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 662))),
                entry(1468472100, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 662))),
                entry(1468472400, new Pair<>(new Extrema<>(0, 62), new Extrema<>(653, 662))),
                entry(1468472700, new Pair<>(new Extrema<>(0, 62), new Extrema<>(654, 662))),
                entry(1468473000, new Pair<>(new Extrema<>(0, 62), new Extrema<>(654, 662))),
                entry(1468473300, new Pair<>(new Extrema<>(0, 62), new Extrema<>(654, 662))),
                entry(1468473600, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468473900, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468474200, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468474500, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468474800, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468475100, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468475400, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468475700, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468476000, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468476300, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 662))),
                entry(1468476600, new Pair<>(new Extrema<>(0, 62), new Extrema<>(655, 663))),
                entry(1468476900, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468477200, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468477500, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468477800, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468478100, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468478400, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468478700, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468479000, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468479300, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468479600, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468479900, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468480200, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468480500, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468480800, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468481100, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468481400, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468481700, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468482000, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468482300, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468482600, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468482900, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468483200, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468483500, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468483800, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468484100, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468484400, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468484700, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468485000, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468485300, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468485600, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468485900, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468486200, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468486500, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468486800, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468487100, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468487400, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468487700, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468488000, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468488300, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468488600, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468488900, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468489200, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468489500, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468489800, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468490100, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468490400, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468490700, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468491000, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468491300, new Pair<>(new Extrema<>(0, 70), new Extrema<>(655, 663))),
                entry(1468491600, new Pair<>(new Extrema<>(0, 70), new Extrema<>(656, 664))),
                entry(1468491900, new Pair<>(new Extrema<>(1, 70), new Extrema<>(656, 664))),
                entry(1468492200, new Pair<>(new Extrema<>(1, 70), new Extrema<>(657, 664))),
                entry(1468492500, new Pair<>(new Extrema<>(1, 70), new Extrema<>(657, 664))),
                entry(1468492800, new Pair<>(new Extrema<>(1, 70), new Extrema<>(657, 664))),
                entry(1468493100, new Pair<>(new Extrema<>(1, 70), new Extrema<>(657, 664))),
                entry(1468493400, new Pair<>(new Extrema<>(1, 70), new Extrema<>(657, 664))),
                entry(1468493700, new Pair<>(new Extrema<>(2, 70), new Extrema<>(657, 664))),
                entry(1468494000, new Pair<>(new Extrema<>(2, 70), new Extrema<>(657, 664))),
                entry(1468494300, new Pair<>(new Extrema<>(2, 70), new Extrema<>(658, 664))),
                entry(1468494600, new Pair<>(new Extrema<>(2, 70), new Extrema<>(658, 664))),
                entry(1468494900, new Pair<>(new Extrema<>(2, 70), new Extrema<>(659, 664))),
                entry(1468495200, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468495500, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468495800, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468496100, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468496400, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468496700, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468497000, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468497300, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468497600, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468497900, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468498200, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468498500, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468498800, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468499100, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468499400, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468499700, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468500000, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468500300, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468500600, new Pair<>(new Extrema<>(2, 70), new Extrema<>(660, 664))),
                entry(1468500900, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468501200, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468501500, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468501800, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468502100, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468502400, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468502700, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468503000, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468503300, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))),
                entry(1468503600, new Pair<>(new Extrema<>(2, 74), new Extrema<>(659, 664))));
    }
}
