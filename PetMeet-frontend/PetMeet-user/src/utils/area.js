/**
 * 中国省市区（全量）- 基于 @vant/area-data
 *
 * - 数据源版本：@vant/area-data（npm 包版本随依赖更新）
 * - 用法：配合 Element Plus `el-cascader` 使用
 *   - `:options="chinaAreaOptions"`
 *   - `:props="chinaAreaCascaderProps"`
 *   - v-model 值为 [provinceCode, cityCode, countyCode]
 */

import { areaList, useCascaderAreaData } from '@vant/area-data'

const { province_list, city_list, county_list } = areaList

export const chinaAreaOptions = useCascaderAreaData()

export const chinaAreaCascaderProps = {
  label: 'text',
  value: 'value',
  children: 'children',
  emitPath: true,
  checkStrictly: false
}

export const chinaAreaNameByCode = (code) => {
  if (!code) return ''
  return province_list[code] || city_list[code] || county_list[code] || ''
}

const buildNameToCodeMap = (list) => {
  const map = new Map()
  Object.entries(list).forEach(([code, name]) => {
    if (!map.has(name)) {
      map.set(name, [])
    }
    map.get(name).push(code)
  })
  return map
}

const provinceNameToCodes = buildNameToCodeMap(province_list)
const cityNameToCodes = buildNameToCodeMap(city_list)
const countyNameToCodes = buildNameToCodeMap(county_list)

export const chinaAreaCodesFromNames = (provinceName, cityName, countyName) => {
  const provinceCodes = provinceNameToCodes.get(provinceName) || []
  const provinceCode = provinceCodes[0]
  if (!provinceCode) return []

  const provincePrefix = provinceCode.slice(0, 2)
  const cityCandidates = (cityNameToCodes.get(cityName) || []).filter(code => code.startsWith(provincePrefix))
  const cityCode = cityCandidates[0]
  if (!cityCode) return [provinceCode]

  const cityPrefix = cityCode.slice(0, 4)
  const countyCandidates = (countyNameToCodes.get(countyName) || []).filter(code => code.startsWith(cityPrefix))
  const countyCode = countyCandidates[0]
  if (!countyCode) return [provinceCode, cityCode]

  return [provinceCode, cityCode, countyCode]
}

